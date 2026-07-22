package com.gridweaver.service;

import com.gridweaver.model.GridNode;
import com.gridweaver.model.ZoneStats;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegionalAnalyticsService {

    private final GridNodeService gridNodeService;

    public RegionalAnalyticsService(GridNodeService gridNodeService) {
        this.gridNodeService = gridNodeService;
    }

    /**
     * Aggregates the live node registry by zoneId.
     * utilizationPercent = consumption / generation * 100, capped at 999
     * to keep the dashboard readable when generation is near zero.
     */
    public List<ZoneStats> computeZoneStats() {
        Map<String, List<GridNode>> byZone = gridNodeService.getAllNodes().stream()
                .filter(n -> n.getZoneId() != null)
                .collect(Collectors.groupingBy(GridNode::getZoneId));

        List<ZoneStats> result = new ArrayList<>();
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