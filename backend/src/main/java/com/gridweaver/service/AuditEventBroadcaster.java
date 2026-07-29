package com.gridweaver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.AuditEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Phase B17 — Registers as an EventAuditService listener and pushes each
 * new audit event over the existing WebSocket channel the instant it's
 * recorded (event-driven, not polled). Kept as its own component so
 * Member A's PowerBalancingBroadcaster (Day 2) is never touched.
 */
@Component
public class AuditEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(AuditEventBroadcaster.class);

    private final EventAuditService eventAuditService;
    private final IoTWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditEventBroadcaster(EventAuditService eventAuditService, IoTWebSocketHandler webSocketHandler) {
        this.eventAuditService = eventAuditService;
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void registerListener() {
        eventAuditService.addListener(this::onAuditEvent);
    }

    private void onAuditEvent(AuditEvent event) {
        if (webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(new AuditEventMessage("AUDIT_EVENT", event));
            webSocketHandler.broadcastToAll(payload);
        } catch (Exception e) {
            log.warn("[AUDIT-BROADCAST-ERROR] {}", e.getMessage());
        }
    }

    private record AuditEventMessage(String type, AuditEvent event) {}
}