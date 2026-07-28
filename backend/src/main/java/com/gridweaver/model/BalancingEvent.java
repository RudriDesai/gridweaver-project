package com.gridweaver.model;

import java.util.UUID;

/**
 * Phase A16 — An executed (not just recommended) power transfer between
 * two zones, published over WebSocket for the map to visualize.
 */
public record BalancingEvent(
        String eventId,
        String fromZone,
        String toZone,
        double amountKw,
        String severity,
        long timestamp
) {
    public static BalancingEvent of(String fromZone, String toZone, double amountKw, String severity) {
        return new BalancingEvent(
                UUID.randomUUID().toString(),
                fromZone,
                toZone,
                amountKw,
                severity,
                System.currentTimeMillis()
        );
    }
}