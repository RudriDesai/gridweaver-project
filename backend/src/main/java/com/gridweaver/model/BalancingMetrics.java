package com.gridweaver.model;

/**
 * Phase A17 — Aggregate balancing metrics computed from current zone
 * imbalance (live) plus the executed-transfer history (rolling window).
 */
public record BalancingMetrics(
        double totalSurplusKw,
        double totalDeficitKw,
        double totalTransferredKw,
        double balancingEfficiencyPercent,
        int activeSurplusZones,
        int activeDeficitZones,
        int executedTransferCount,
        long windowMs,
        long timestamp
) {}