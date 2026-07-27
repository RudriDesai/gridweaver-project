package com.gridweaver.model;

/**
 * Represents a single surplus→deficit balancing suggestion between two zones.
 * Immutable — computed fresh on every request, never persisted.
 */
public record BalancingRecommendation(
        String fromZone,
        String toZone,
        double surplus,
        double deficit,
        double recommendedTransfer,
        String severity   // "LOW" | "MEDIUM" | "HIGH"
) {}