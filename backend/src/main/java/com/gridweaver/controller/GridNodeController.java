package com.gridweaver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.GridNode;
import com.gridweaver.service.GridNodeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class GridNodeController {

    private final GridNodeService gridNodeService;
    private final IoTWebSocketHandler webSocketHandler;

    public GridNodeController(GridNodeService gridNodeService,
                              IoTWebSocketHandler webSocketHandler) {
        this.gridNodeService = gridNodeService;
        this.webSocketHandler = webSocketHandler;
    }

    // ── Health Check ────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }

    // ── Node APIs ───────────────────────────────────────
    @GetMapping("/nodes")
    public ResponseEntity<List<GridNode>> getAllNodes() {
        return ResponseEntity.ok(gridNodeService.getAllNodes());
    }

    @GetMapping("/nodes/init/{count}")
    public ResponseEntity<List<GridNode>> initNodes(@PathVariable int count) {
        return ResponseEntity.ok(gridNodeService.initializeMockNodes(count));
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<GridNode> getNodeById(@PathVariable String id) {
        GridNode node = gridNodeService.getNodeById(id);
        if (node == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(node);
    }

    // ── WebSocket Metrics ───────────────────────────────
    @GetMapping("/ws/metrics")
    public ResponseEntity<Map<String, Object>> getWebSocketMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeConnections", 
            webSocketHandler.getActiveConnectionCount());
        metrics.put("totalMessagesReceived", 
            webSocketHandler.getTotalMessagesReceived());
        metrics.put("totalConnectionsEver", 
            webSocketHandler.getTotalConnectionsEver());
        metrics.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(metrics);
    }
}