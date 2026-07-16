package com.gridweaver.service;

import java.util.List;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.GridNode;

@Component
public class LiveUpdateBroadcaster {

    private static final Logger log =
            LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private final GridNodeService gridNodeService;
    private final IoTWebSocketHandler webSocketHandler;
    private final BatteryStateService batteryStateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LiveUpdateBroadcaster(
            GridNodeService gridNodeService,
            IoTWebSocketHandler webSocketHandler,
            BatteryStateService batteryStateService) {

        this.gridNodeService = gridNodeService;
        this.webSocketHandler = webSocketHandler;
        this.batteryStateService = batteryStateService;
    }

    @PostConstruct
    public void registerStateChangeListener() {

        batteryStateService.addListener((nodeId, oldState, newState) -> {

            try {

                GridNode node = gridNodeService.getNodeById(nodeId);

                if (node == null) {
                    return;
                }

                String payload = objectMapper.writeValueAsString(
                        new NodeUpdateMessage("NODE_UPDATE", "PARTIAL", List.of(node))
                );

                webSocketHandler.broadcastToAll(payload);

                log.info(
                        "[LIVE-UPDATE] {} : {} -> {}",
                        nodeId,
                        oldState,
                        newState
                );

            } catch (Exception e) {

                log.warn("[BROADCAST-ERROR] on state change: {}", e.getMessage());
            }
        });
    }

    @Scheduled(fixedRate = 2000)
    public void broadcastNodeUpdates() {

        if (webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }

        try {

            List<GridNode> nodes = gridNodeService.getAllNodes();

            if (nodes.isEmpty()) {
                return;
            }

            String payload = objectMapper.writeValueAsString(
                    new NodeUpdateMessage(
                            "NODE_UPDATE",
                            "FULL",
                            nodes
                    )
            );

            webSocketHandler.broadcastToAll(payload);

        } catch (Exception e) {

            log.warn("[BROADCAST-ERROR] {}", e.getMessage());
        }
    }

    private record NodeUpdateMessage(
            String type,
            String updateType,
            List<GridNode> nodes
    ) {
    }
}