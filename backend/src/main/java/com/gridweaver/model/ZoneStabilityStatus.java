package com.gridweaver.model;

/**
 * Phase A18 — Live per-zone stability snapshot (polled by the map overlay),
 * distinct from GridStabilityAlert which only fires on threshold crossing.
 */
public record ZoneStabilityStatus(
        String zoneId,
        boolean stable,
        double utilizationPercent,
        int faultNodeCount,
        int totalNodeCount,
        String severity   // "NONE" | "LOW" | "MEDIUM" | "HIGH"
) {}