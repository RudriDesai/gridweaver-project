package com.gridweaver.model;

import java.util.UUID;

public record AuditEvent(
        String eventId,
        String nodeId,
        String previousState,
        String newState,
        String reason,
        long timestamp
) {
    public static AuditEvent of(String nodeId, String previousState, String newState, String reason) {
        return new AuditEvent(
                UUID.randomUUID().toString(),
                nodeId,
                previousState,
                newState,
                reason,
                System.currentTimeMillis()
        );
    }
}