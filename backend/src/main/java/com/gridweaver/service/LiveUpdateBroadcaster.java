package com.gridweaver.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.GridNode;

/**
 * Periodically pushes the current node registry to all connected
 * WebSocket clients, so the frontend no longer has to poll REST.
 */
@Component
public class LiveUpdateBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private final GridNodeService gridNodeService;
    private final IoTWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LiveUpdateBroadcaster(GridNodeService gridNodeService,
                                 IoTWebSocketHandler webSocketHandler) {
        this.gridNodeService = gridNodeService;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedRate = 2000)
    public void broadcastNodeUpdates() {
        if (webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }
        try {
            List<GridNode> nodes = gridNodeService.getAllNodes();
            if (nodes.isEmpty()) return;

            String payload = objectMapper.writeValueAsString(
                    new NodeUpdateMessage("NODE_UPDATE", nodes)
            );
            webSocketHandler.broadcastToAll(payload);
        } catch (Exception e) {
            log.warn("[BROADCAST-ERROR] {}", e.getMessage());
        }
    }
    private record NodeUpdateMessage(String type, List<GridNode> nodes) {}
}