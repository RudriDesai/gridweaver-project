package com.gridweaver.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gridweaver.model.GridNode;
import com.gridweaver.service.BatteryStateService;
import com.gridweaver.kafka.dto.TelemetryEvent;
/**
 * Registry and management service for all GridNodes in the microgrid.
 *
 * Uses ConcurrentHashMap as the in-memory store.
 * Week 3: This will be replaced by Kafka-backed state store.
 */
@Service
public class GridNodeService {

    private static final Logger log =
            LoggerFactory.getLogger(GridNodeService.class);

    // Thread-safe in-memory node registry
    // Key: nodeId, Value: GridNode
    private final ConcurrentHashMap<String, GridNode> nodeRegistry =
            new ConcurrentHashMap<>();
    
    // Battery State Machine service
    private final BatteryStateService batteryStateService;
    
    // Valid status values — enforced on registration
    private static final List<String> VALID_STATUSES =
            List.of("CHARGING", "DISCHARGING", "IDLE", "FAULT");
    
    private static final String[] ZONES = {"ZONE-A", "ZONE-B", "ZONE-C", "ZONE-D"};
    
    public GridNodeService(BatteryStateService batteryStateService) {
        this.batteryStateService = batteryStateService;
    }
    // ── Registry Operations ───────────────────────────

    /**
     * Register a new node into the microgrid.
     * Throws if nodeId already exists or status is invalid.
     */
    public GridNode registerNode(GridNode node) {
        if (nodeRegistry.containsKey(node.getNodeId())) {
            throw new IllegalArgumentException(
                    "Node already registered: " + node.getNodeId());
        }
        if (!VALID_STATUSES.contains(node.getStatus())) {
            throw new IllegalArgumentException(
                    "Invalid status: " + node.getStatus()
                            + " | Valid: " + VALID_STATUSES);
        }
        nodeRegistry.put(node.getNodeId(), node);
        log.info("[REGISTRY] Registered node: {}", node.getNodeId());
        return node;
    }

    /**
     * Lookup a node by its ID.
     * Returns null if not found (controller handles 404).
     */
    public GridNode getNodeById(String nodeId) {
        return nodeRegistry.get(nodeId);
    }

    /**
     * Returns all registered nodes as a list.
     */
    public List<GridNode> getAllNodes() {
        return new ArrayList<>(nodeRegistry.values());
    }

    /**
     * Returns count of registered nodes.
     */
    public int getNodeCount() {
        return nodeRegistry.size();
    }
    
    /**
     * Applies incoming telemetry from a simulated/real IoT device to the
     * matching node: recomputes grid load, re-evaluates the state machine,
     * and updates the registry. Creates the node on first contact if unseen.
     */
    public GridNode applyTelemetry(String nodeId, double powerOutput) {
        GridNode node = nodeRegistry.get(nodeId);

        // Derive a synthetic grid load from power output (0-100 scale)
        double gridLoad = Math.min(100.0, Math.round(powerOutput * 10.0) / 10.0);
        String newStatus = batteryStateService.evaluate(nodeId, gridLoad).name();

        if (node == null) {

            // Spread new nodes around the map instead of placing them all
            // at exactly the same coordinates.
            double baseLat = 51.505;
            double baseLng = -0.09;

            double lat = baseLat + (Math.random() * 0.2 - 0.1);
            double lng = baseLng + (Math.random() * 0.2 - 0.1);

            node = new GridNode(
                    nodeId,
                    lat,
                    lng,
                    newStatus,
                    powerOutput,
                    gridLoad
            );

            nodeRegistry.put(nodeId, node);
            log.info("[TELEMETRY] Registered new node from telemetry: {}", nodeId);

        } else {
            node.setPowerOutput(powerOutput);
            node.setGridLoad(gridLoad);
            node.setStatus(newStatus);
            node.setTimestamp(System.currentTimeMillis());
        }

        log.debug("[TELEMETRY] node={} load={} -> status={}", nodeId, gridLoad, newStatus);
        return node;
    }
    
    /**
     * Phase A12/B11: full-fidelity telemetry path used by TelemetryConsumerService.
     * Carries zoneId/generation/consumption through to the registry so
     * RegionalAnalyticsService and the heatmap have real data to read.
     */
    public GridNode applyTelemetry(TelemetryEvent event) {
        GridNode node = nodeRegistry.get(event.nodeId());

        double gridLoad = Math.min(100.0, Math.round(event.generation() * 10.0) / 10.0);
        String newStatus = batteryStateService.evaluate(event.nodeId(), gridLoad).name();

        if (node == null) {
            double baseLat = 51.505, baseLng = -0.09;
            double lat = baseLat + (Math.random() * 0.2 - 0.1);
            double lng = baseLng + (Math.random() * 0.2 - 0.1);

            node = new GridNode(event.nodeId(), lat, lng, newStatus,
                    event.generation(), gridLoad,
                    event.zoneId(), event.generation(), event.consumption());
            nodeRegistry.put(event.nodeId(), node);
            log.info("[TELEMETRY] Registered new node from Kafka event: {}", event.nodeId());
        } else {
            node.setPowerOutput(event.generation());
            node.setGridLoad(gridLoad);
            node.setStatus(newStatus);
            node.setZoneId(event.zoneId());
            node.setGeneration(event.generation());
            node.setConsumption(event.consumption());
            node.setTimestamp(System.currentTimeMillis());
        }
        return node;
    }

    // ── Mock Data Initialization ──────────────────────

    /**
     * Generates and registers N mock IoT nodes.
     * Used for Week 1 testing and frontend development.
     * Week 2: Real nodes will register themselves via WebSocket.
     */
    public List<GridNode> initializeMockNodes(int count) {

        nodeRegistry.clear();

        // Mock city center: London
        double baseLat = 51.505;
        double baseLng = -0.09;

        for (int i = 0; i < count; i++) {

            // Spread nodes across geographic area
            double lat = baseLat + (Math.random() * 0.2 - 0.1);
            double lng = baseLng + (Math.random() * 0.2 - 0.1);

            double power = Math.round(Math.random() * 100 * 10.0) / 10.0;
            double load = Math.round(Math.random() * 100 * 10.0) / 10.0;

            String nodeId = "NODE-" + String.format("%04d", i + 1);
            
            String zoneId = ZONES[i % ZONES.length];

            String status = batteryStateService
                    .evaluate(nodeId, load)
                    .name();

            GridNode node = new GridNode(
                    nodeId,
                    lat,
                    lng,
                    status,
                    power,
                    load,
                    zoneId,
                    power,
                    power * 0.6
            );

            nodeRegistry.put(node.getNodeId(), node);
        }

        log.info("[REGISTRY] Initialized {} mock nodes", nodeRegistry.size());
        return new ArrayList<>(nodeRegistry.values());
    }
}