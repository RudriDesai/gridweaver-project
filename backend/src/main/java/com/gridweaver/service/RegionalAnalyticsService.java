package com.gridweaver.service;

import com.gridweaver.model.GridNode;
import com.gridweaver.model.ZoneStats;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
    
    /**
     * Phase A19 — Optimization: computeZoneStats() is now called multiple times
     * per broadcast cycle (PowerBalancingService.executeBalancing() +
     * computeMetrics(), GridStabilityService, AnalyticsController polling) —
     * each doing a full stream/groupingBy pass over every node. A short-TTL
     * cache collapses concurrent/near-simultaneous calls into a single
     * recomputation without ever returning data older than the TTL.
     */
    @Service
    public class RegionalAnalyticsService {

        private static final long CACHE_TTL_MS = 1000L; // matches fastest poll interval

        private final GridNodeService gridNodeService;

        private volatile List<ZoneStats> cachedStats = null;
        private volatile long cacheTimestamp = 0L;

        public RegionalAnalyticsService(GridNodeService gridNodeService) {
            this.gridNodeService = gridNodeService;
        }

        public List<ZoneStats> computeZoneStats() {
            long now = System.currentTimeMillis();

            // Fast path: serve cached result if still fresh — avoids redundant
            // stream/groupingBy passes when multiple services call this within
            // the same tick (common during a balancing + stability cycle).
            List<ZoneStats> cached = cachedStats;
            if (cached != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
                return cached;
            }

            List<ZoneStats> fresh = computeZoneStatsUncached();
            cachedStats = fresh;
            cacheTimestamp = now;
            return fresh;
        }

        private List<ZoneStats> computeZoneStatsUncached() {
            List<GridNode> allNodes = gridNodeService.getAllNodes();

            // Edge case: no nodes registered yet
            if (allNodes == null || allNodes.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, List<GridNode>> byZone = allNodes.stream()
                    .filter(n -> n.getZoneId() != null)
                    .collect(Collectors.groupingBy(GridNode::getZoneId));

            List<ZoneStats> result = new ArrayList<>(byZone.size());
            for (var entry : byZone.entrySet()) {
                List<GridNode> nodes = entry.getValue();
                double totalGen = nodes.stream().mapToDouble(GridNode::getGeneration).sum();
                double totalCons = nodes.stream().mapToDouble(GridNode::getConsumption).sum();
                double utilization = totalGen > 0
                        ? Math.min(999.0, Math.round((totalCons / totalGen) * 1000.0) / 10.0)
                        : 0.0;

                result.add(new ZoneStats(entry.getKey(), nodes.size(), totalGen, totalCons, utilization));
            }
            result.sort(Comparator.comparing(ZoneStats::zoneId));
            return result;
        }
    }