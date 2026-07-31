package com.gridweaver.model;

public record AuditHealthStatus(
        String status,               // "UP" | "DEGRADED"
        long totalEventsStored,
        double eventsPerHourRecent,
        int activeWebSocketConnections,
        int pendingBroadcastQueueSize,
        long timestamp
) {}