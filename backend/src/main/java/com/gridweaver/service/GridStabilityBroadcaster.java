package com.gridweaver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.GridStabilityAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Phase A18 — Runs stability detection on a fixed interval and broadcasts
 * any new alerts. Separate component so Day 2/3's broadcasters
 * (PowerBalancingBroadcaster, AuditEventBroadcaster) are never touched.
 */
@Component
public class GridStabilityBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(GridStabilityBroadcaster.class);

    private final GridStabilityService gridStabilityService;
    private final IoTWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GridStabilityBroadcaster(GridStabilityService gridStabilityService,
                                     IoTWebSocketHandler webSocketHandler) {
        this.gridStabilityService = gridStabilityService;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedRateString = "${gridweaver.stability.interval-ms:5000}")
    public void runStabilityCycle() {
        if (webSocketHandler.getActiveConnectionCount() == 0) return;

        try {
            List<GridStabilityAlert> alerts = gridStabilityService.detectAndRecoverAlerts();
            if (alerts.isEmpty()) return;

            String payload = objectMapper.writeValueAsString(
                    new StabilityAlertMessage("STABILITY_ALERT", alerts));
            webSocketHandler.broadcastToAll(payload);

        } catch (Exception e) {
            log.warn("[STABILITY-BROADCAST-ERROR] {}", e.getMessage());
        }
    }

    private record StabilityAlertMessage(String type, List<GridStabilityAlert> alerts) {}
}