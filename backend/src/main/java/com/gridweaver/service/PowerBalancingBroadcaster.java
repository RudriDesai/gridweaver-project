package com.gridweaver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.BalancingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase A16 — Runs the balancing engine on a fixed interval and broadcasts
 * any executed transfers over the existing WebSocket channel.
 *
 * Kept as its own component (rather than added to LiveUpdateBroadcaster)
 * so Member A and Member B's Day 2 work never touch the same file.
 */
@Component
public class PowerBalancingBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(PowerBalancingBroadcaster.class);

    private final PowerBalancingService powerBalancingService;
    private final IoTWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PowerBalancingBroadcaster(PowerBalancingService powerBalancingService,
                                      IoTWebSocketHandler webSocketHandler) {
        this.powerBalancingService = powerBalancingService;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedRateString = "${gridweaver.balancing.interval-ms:5000}")
    public void runBalancingCycle() {
        if (webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }

        try {
            List<BalancingEvent> executed = powerBalancingService.executeBalancing();
            if (executed.isEmpty()) {
                return;
            }

            String payload = objectMapper.writeValueAsString(
                    new BalancingEventMessage("BALANCING_EVENT", executed));

            webSocketHandler.broadcastToAll(payload);
            log.info("[BALANCING-BROADCAST] {} transfer(s) sent", executed.size());

        } catch (Exception e) {
            log.warn("[BALANCING-BROADCAST-ERROR] {}", e.getMessage());
        }
    }

    private record BalancingEventMessage(String type, List<BalancingEvent> events) {}
}