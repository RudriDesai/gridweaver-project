package com.gridweaver.service;

import com.gridweaver.model.BalancingEvent;
import com.gridweaver.model.GridNode;
import com.gridweaver.model.GridStabilityAlert;
import com.gridweaver.model.ZoneStabilityStatus;
import com.gridweaver.model.ZoneStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class GridStabilityService {

    private static final int MAX_ALERT_HISTORY = 500;

    @Value("${gridweaver.stability.overload-threshold-percent:90}")
    private double overloadThreshold;

    @Value("${gridweaver.stability.fault-cluster-size:2}")
    private int faultClusterSize;

    private final RegionalAnalyticsService regionalAnalyticsService;
    private final GridNodeService gridNodeService;
    private final PowerBalancingService powerBalancingService;

    private final Deque<GridStabilityAlert> alertHistory =
            new ConcurrentLinkedDeque<>();

    /*
     * Keeps track of zones that are already unstable.
     * Prevents duplicate alerts every scheduler cycle.
     */
    private final Set<String> activeAlertZones =
            ConcurrentHashMap.newKeySet();

    public GridStabilityService(
            RegionalAnalyticsService regionalAnalyticsService,
            GridNodeService gridNodeService,
            PowerBalancingService powerBalancingService) {

        this.regionalAnalyticsService = regionalAnalyticsService;
        this.gridNodeService = gridNodeService;
        this.powerBalancingService = powerBalancingService;
    }

    /**
     * Current live status of every zone.
     */
    public List<ZoneStabilityStatus> getCurrentZoneStability() {

        List<ZoneStats> zones =
                regionalAnalyticsService.computeZoneStats();

        Map<String, Long> faultCounts =
                gridNodeService.getAllNodes()
                        .stream()
                        .filter(n -> "FAULT".equals(n.getStatus()))
                        .collect(Collectors.groupingBy(
                                GridNode::getZoneId,
                                Collectors.counting()));

        List<ZoneStabilityStatus> result = new ArrayList<>();

        for (ZoneStats zone : zones) {

            int faults =
                    faultCounts.getOrDefault(zone.zoneId(), 0L).intValue();

            boolean overloaded =
                    zone.utilizationPercent() >= overloadThreshold;

            boolean faultCluster =
                    faults >= faultClusterSize;

            boolean stable =
                    !(overloaded || faultCluster);

            String severity = "NONE";

            if (overloaded && faultCluster) {
                severity = "HIGH";
            } else if (overloaded) {
                severity = "MEDIUM";
            } else if (faultCluster) {
                severity = faults > faultClusterSize
                        ? "HIGH"
                        : "MEDIUM";
            }

            result.add(
                    new ZoneStabilityStatus(
                            zone.zoneId(),
                            stable,
                            zone.utilizationPercent(),
                            faults,
                            zone.nodeCount(),
                            severity
                    )
            );
        }

        return result;
    }

    /**
     * Detects new instability.
     * Only returns NEW alerts.
     */
    public synchronized List<GridStabilityAlert> detectAndRecoverAlerts() {

        List<ZoneStabilityStatus> statuses =
                getCurrentZoneStability();

        List<GridStabilityAlert> alerts =
                new ArrayList<>();

        boolean shouldRebalance = false;

        for (ZoneStabilityStatus status : statuses) {

            if (status.stable()) {

                activeAlertZones.remove(status.zoneId());

                continue;
            }

            /*
             * Zone already unstable.
             * Don't create another alert.
             */
            if (!activeAlertZones.add(status.zoneId())) {
                continue;
            }

            shouldRebalance = true;

            if (status.utilizationPercent() >= overloadThreshold) {

                alerts.add(
                        GridStabilityAlert.of(
                                status.zoneId(),
                                "OVERLOAD",
                                status.severity(),
                                "Zone utilization exceeded "
                                        + overloadThreshold + "%",
                                false
                        )
                );
            }

            if (status.faultNodeCount() >= faultClusterSize) {

                alerts.add(
                        GridStabilityAlert.of(
                                status.zoneId(),
                                "FAULT_CLUSTER",
                                status.severity(),
                                "Multiple faulted nodes detected",
                                false
                        )
                );
            }
        }

        /*
         * Trigger balancing only once.
         */
        if (shouldRebalance) {

            List<BalancingEvent> executed =
                    powerBalancingService.executeBalancing();

            boolean rebalanced =
                    !executed.isEmpty();

            for (int i = 0; i < alerts.size(); i++) {

                GridStabilityAlert old = alerts.get(i);

                alerts.set(
                        i,
                        new GridStabilityAlert(
                                old.alertId(),
                                old.zoneId(),
                                old.alertType(),
                                old.severity(),
                                old.message(),
                                rebalanced,
                                old.timestamp()
                        )
                );
            }
        }

        alerts.forEach(alertHistory::addFirst);

        while (alertHistory.size() > MAX_ALERT_HISTORY) {
            alertHistory.removeLast();
        }

        return alerts;
    }

    /**
     * Recent alerts.
     */
    public List<GridStabilityAlert> getRecentAlerts(int limit) {

        return alertHistory
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}