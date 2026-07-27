package com.gridweaver.service;

import com.gridweaver.model.BalancingRecommendation;
import com.gridweaver.model.ZoneStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    private final RegionalAnalyticsService regionalAnalyticsService;

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

    private String severity(double deficit) {
        if (deficit > 20.0) return "HIGH";
        if (deficit > 8.0) return "MEDIUM";
        return "LOW";
    }
}