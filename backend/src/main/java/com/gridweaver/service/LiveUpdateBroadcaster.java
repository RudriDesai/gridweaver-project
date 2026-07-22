package com.gridweaver.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.GridNode;
import com.gridweaver.model.ZoneStats;

@Component
public class LiveUpdateBroadcaster {

    private static final Logger log =
            LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private final GridNodeService gridNodeService;
    private final IoTWebSocketHandler webSocketHandler;
    private final BatteryStateService batteryStateService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RegionalAnalyticsService regionalAnalyticsService;

    private final ConcurrentLinkedQueue<GridNode> pendingChanges =
            new ConcurrentLinkedQueue<>();

    public LiveUpdateBroadcaster(
            GridNodeService gridNodeService,
            IoTWebSocketHandler webSocketHandler,
            BatteryStateService batteryStateService,
            RegionalAnalyticsService regionalAnalyticsService) {

        this.gridNodeService = gridNodeService;
        this.webSocketHandler = webSocketHandler;
        this.batteryStateService = batteryStateService;
        this.regionalAnalyticsService = regionalAnalyticsService;
    }

    @PostConstruct
    public void registerStateChangeListener() {

        batteryStateService.addListener((nodeId, oldState, newState) -> {

            GridNode node = gridNodeService.getNodeById(nodeId);

            if (node != null) {
                pendingChanges.add(node);

                log.info(
                        "[QUEUE] {} : {} -> {}",
                        nodeId,
                        oldState,
                        newState
                );
            }
        });
    }

    /**
     * Day 5
     * Flush queued updates every 300ms instead of
     * sending one WebSocket frame per node.
     */
    @Scheduled(fixedRate = 300)
    public void flushPendingChanges() {

        if (pendingChanges.isEmpty()) {
            return;
        }

        if (webSocketHandler.getActiveConnectionCount() == 0) {
            pendingChanges.clear();
            return;
        }

        LinkedHashMap<String, GridNode> deduplicated =
                new LinkedHashMap<>();

        GridNode node;

        while ((node = pendingChanges.poll()) != null) {
            deduplicated.put(node.getNodeId(), node);
        }

        List<GridNode> batch =
                new ArrayList<>(deduplicated.values());

        try {

            String payload = objectMapper.writeValueAsString(
                    new NodeUpdateMessage(
                            "NODE_UPDATE",
                            "PARTIAL",
                            batch
                    )
            );

            webSocketHandler.broadcastToAll(payload);

            log.info(
                    "[BATCH] Broadcast {} node updates",
                    batch.size()
            );

        } catch (Exception e) {

            log.warn(
                    "[BROADCAST-ERROR] partial batch: {}",
                    e.getMessage()
            );
        }
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

            log.warn(
                    "[BROADCAST-ERROR] full sync: {}",
                    e.getMessage()
            );
        }
    }
    /**
     * Phase B12
     * Broadcast live zone analytics for the GIS heatmap.
     */
    @Scheduled(fixedRate = 2000)
    public void broadcastZoneUpdates() {

        if (webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }

        try {

            List<ZoneStats> zoneStats =
                    regionalAnalyticsService.computeZoneStats();

            if (zoneStats.isEmpty()) {
                return;
            }

            String payload = objectMapper.writeValueAsString(
                    new ZoneUpdateMessage(
                            "ZONE_UPDATE",
                            zoneStats
                    )
            );

            webSocketHandler.broadcastToAll(payload);

        } catch (Exception e) {

            log.warn(
                    "[BROADCAST-ERROR] zone update: {}",
                    e.getMessage()
            );
        }
    }
    private record NodeUpdateMessage(
            String type,
            String updateType,
            List<GridNode> nodes
    ) {
    }
    private record ZoneUpdateMessage(
            String type,
            List<ZoneStats> zones
    ) {
    }
}