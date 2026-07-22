package com.gridweaver.model;

public record ZoneStats(
        String zoneId,
        int nodeCount,
        double totalGeneration,
        double totalConsumption,
        double utilizationPercent
) {}