package com.gridweaver.model;

import java.util.UUID;

/**
 * Phase A18 — A single stability alert raised for a zone: either it's
 * overloaded (utilization past threshold) or has a cluster of faulted nodes.
 */
public record GridStabilityAlert(
        String alertId,
        String zoneId,
        String alertType,       // "OVERLOAD" | "FAULT_CLUSTER"
        String severity,        // "LOW" | "MEDIUM" | "HIGH"
        String message,
        boolean rebalanceTriggered,
        long timestamp
) {
    public static GridStabilityAlert of(String zoneId, String alertType, String severity,
                                         String message, boolean rebalanceTriggered) {
        return new GridStabilityAlert(
                UUID.randomUUID().toString(), zoneId, alertType, severity,
                message, rebalanceTriggered, System.currentTimeMillis());
    }
}