package com.gridweaver.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gridweaver.model.GridNode;

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

    // Valid status values — enforced on registration
    private static final List<String> VALID_STATUSES =
            List.of("CHARGING", "DISCHARGING", "IDLE", "FAULT");

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

    // ── Mock Data Initialization ──────────────────────

    /**
     * Generates and registers N mock IoT nodes.
     * Used for Week 1 testing and frontend development.
     * Week 2: Real nodes will register themselves via WebSocket.
     */
    public List<GridNode> initializeMockNodes(int count) {
        nodeRegistry.clear();

        String[] statuses = {"CHARGING", "DISCHARGING", "IDLE", "FAULT"};

        // Mock city center: London
        double baseLat = 51.505;
        double baseLng = -0.09;

        for (int i = 0; i < count; i++) {

            // Spread nodes across geographic area
            double lat = baseLat + (Math.random() * 0.2 - 0.1);
            double lng = baseLng + (Math.random() * 0.2 - 0.1);

            String status = statuses[i % statuses.length];
            double power  = Math.round(Math.random() * 100 * 10.0) / 10.0;
            double load   = Math.round(Math.random() * 100 * 10.0) / 10.0;

            GridNode node = new GridNode(
                    "NODE-" + String.format("%04d", i + 1),
                    lat, lng, status, power, load
            );

            nodeRegistry.put(node.getNodeId(), node);
        }

        log.info("[REGISTRY] Initialized {} mock nodes", nodeRegistry.size());
        return new ArrayList<>(nodeRegistry.values());
    }
}