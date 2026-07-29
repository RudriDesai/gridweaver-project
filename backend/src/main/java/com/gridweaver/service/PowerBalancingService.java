package com.gridweaver.service;

import com.gridweaver.model.BalancingMetrics;
import com.gridweaver.model.BalancingEvent;
import com.gridweaver.model.BalancingRecommendation;
import com.gridweaver.model.ZoneStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Phase A15 — Regional Power Balancing Engine.
 *
 * Reads live ZoneStats (already computed by RegionalAnalyticsService) and
 * detects zones running a surplus (generation > consumption) vs zones
 * running a deficit (consumption > generation), then proposes a transfer
 * amount from each surplus zone to each deficit zone.
 *
 * Read-only in Day 1 — no state mutation, no WebSocket events yet.
 * Actual transfer execution + WebSocket broadcast is Phase A16 (Day 2).
 */
@Service
public class PowerBalancingService {

    private static final Logger log = LoggerFactory.getLogger(PowerBalancingService.class);

    // A zone only counts as surplus/deficit once the gap exceeds this,
    // to avoid noisy micro-recommendations on near-balanced zones.
    private static final double IMBALANCE_THRESHOLD_KW = 1.0;
    private static final int MAX_EXECUTION_HISTORY = 500;
    private static final long METRICS_WINDOW_MS = 5 * 60 * 1000L; // 5-minute rolling window

    private final RegionalAnalyticsService regionalAnalyticsService;

 // Phase A16 — configurable, read from application.properties
    @Value("${gridweaver.balancing.threshold-kw:5.0}")
    private double executionThresholdKw;

    // Phase A16 — in-memory record of transfers actually executed
    private final Deque<BalancingEvent> executionHistory = new ConcurrentLinkedDeque<>();

    public PowerBalancingService(RegionalAnalyticsService regionalAnalyticsService) {
        this.regionalAnalyticsService = regionalAnalyticsService;
    }

    /**
     * Computes balancing recommendations by pairing surplus zones with
     * deficit zones, largest imbalance first.
     */
    public List<BalancingRecommendation> computeRecommendations() {
        List<ZoneStats> zones = regionalAnalyticsService.computeZoneStats();

        List<ZoneStats> surplusZones = new ArrayList<>();
        List<ZoneStats> deficitZones = new ArrayList<>();

        for (ZoneStats z : zones) {
            double net = z.totalGeneration() - z.totalConsumption();
            if (net > IMBALANCE_THRESHOLD_KW) {
                surplusZones.add(z);
            } else if (net < -IMBALANCE_THRESHOLD_KW) {
                deficitZones.add(z);
            }
        }

        // Largest surplus/deficit first — biggest imbalances get resolved first
        surplusZones.sort((a, b) -> Double.compare(
                (b.totalGeneration() - b.totalConsumption()),
                (a.totalGeneration() - a.totalConsumption())));
        deficitZones.sort((a, b) -> Double.compare(
                (a.totalGeneration() - a.totalConsumption()),
                (b.totalGeneration() - b.totalConsumption())));

        List<BalancingRecommendation> recommendations = new ArrayList<>();

        for (ZoneStats surplusZone : surplusZones) {
            double surplus = surplusZone.totalGeneration() - surplusZone.totalConsumption();

            for (ZoneStats deficitZone : deficitZones) {
                double deficit = deficitZone.totalConsumption() - deficitZone.totalGeneration();

                double transfer = Math.round(Math.min(surplus, deficit) * 10.0) / 10.0;
                if (transfer <= 0) {
                    continue;
                }

                recommendations.add(new BalancingRecommendation(
                        surplusZone.zoneId(),
                        deficitZone.zoneId(),
                        Math.round(surplus * 10.0) / 10.0,
                        Math.round(deficit * 10.0) / 10.0,
                        transfer,
                        severity(deficit)
                ));
            }
        }

        log.debug("[BALANCING] Computed {} recommendations from {} surplus / {} deficit zones",
                recommendations.size(), surplusZones.size(), deficitZones.size());

        return recommendations;
    }
    
    /**
     * Phase A16 — Executes balancing: any recommendation whose transfer
     * amount clears the configurable threshold is "executed" (recorded as
     * a BalancingEvent for WebSocket broadcast + later analytics).
     *
     * Day 2 is intentionally read/record-only against node telemetry —
     * it does not mutate GridNode generation/consumption, since telemetry
     * is owned by the Kafka pipeline (Phase A11/B11). This avoids fighting
     * the simulator for ownership of those fields while still giving a
     * real, queryable execution feed for the map and later analytics.
     */
    public List<BalancingEvent> executeBalancing() {
        List<BalancingRecommendation> recommendations = computeRecommendations();

        List<BalancingEvent> executed = recommendations.stream()
                .filter(r -> r.recommendedTransfer() >= executionThresholdKw)
                .map(r -> BalancingEvent.of(r.fromZone(), r.toZone(), r.recommendedTransfer(), r.severity()))
                .collect(Collectors.toList());

        executed.forEach(e -> {
            executionHistory.addFirst(e);
            log.info("[BALANCING-EXEC] {} -> {} : {} kW ({})",
                    e.fromZone(), e.toZone(), e.amountKw(), e.severity());
        });

        while (executionHistory.size() > MAX_EXECUTION_HISTORY) {
            executionHistory.removeLast();
        }

        return executed;
    }

    public List<BalancingEvent> getRecentExecutions(int limit) {
        return executionHistory.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Phase A17 — Computes balancing analytics:
     *  - totalSurplusKw / totalDeficitKw: current live imbalance across zones
     *  - totalTransferredKw: sum of executed transfers within the rolling window
     *  - balancingEfficiencyPercent: how much of the addressable deficit has
     *    actually been resolved by executed transfers in that window
     */
    public BalancingMetrics computeMetrics() {
        List<ZoneStats> zones = regionalAnalyticsService.computeZoneStats();

        double totalSurplus = 0;
        double totalDeficit = 0;
        int surplusZoneCount = 0;
        int deficitZoneCount = 0;

        for (ZoneStats z : zones) {
            double net = z.totalGeneration() - z.totalConsumption();
            if (net > IMBALANCE_THRESHOLD_KW) {
                totalSurplus += net;
                surplusZoneCount++;
            } else if (net < -IMBALANCE_THRESHOLD_KW) {
                totalDeficit += Math.abs(net);
                deficitZoneCount++;
            }
        }

        long now = System.currentTimeMillis();
        long windowStart = now - METRICS_WINDOW_MS;

        List<BalancingEvent> windowEvents = executionHistory.stream()
                .filter(e -> e.timestamp() >= windowStart)
                .collect(Collectors.toList());

        double totalTransferred = windowEvents.stream()
                .mapToDouble(BalancingEvent::amountKw)
                .sum();

        // Efficiency: portion of current+recent deficit that transfers have
        // actually resolved. Capped at 100% and guarded against divide-by-zero
        // when the grid is already balanced (deficit == 0 -> perfect score).
        double addressableDeficit = totalDeficit + totalTransferred;
        double efficiency = addressableDeficit <= 0
                ? 100.0
                : Math.min(100.0, (totalTransferred / addressableDeficit) * 100.0);

        return new BalancingMetrics(
                round1(totalSurplus),
                round1(totalDeficit),
                round1(totalTransferred),
                round1(efficiency),
                surplusZoneCount,
                deficitZoneCount,
                windowEvents.size(),
                METRICS_WINDOW_MS,
                now
        );
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String severity(double deficit) {
        if (deficit > 20.0) return "HIGH";
        if (deficit > 8.0) return "MEDIUM";
        return "LOW";
    }
}