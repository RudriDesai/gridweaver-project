package com.gridweaver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsEnabled;

    public GridNodeController(GridNodeService gridNodeService,
                              IoTWebSocketHandler webSocketHandler) {
        this.gridNodeService = gridNodeService;
        this.webSocketHandler = webSocketHandler;
    }

    // ── Health Check ────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("virtualThreadsEnabled", virtualThreadsEnabled);
        return ResponseEntity.ok(response);
    }

    // ── Concurrency / Memory Audit (Week 1 Mid-Project Review) ──
    @GetMapping("/concurrency-audit")
    public ResponseEntity<Map<String, Object>> concurrencyAudit() {
        Runtime runtime = Runtime.getRuntime();

        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        Map<String, Object> audit = new HashMap<>();
        audit.put("activeWebSocketConnections", webSocketHandler.getActiveConnectionCount());
        audit.put("liveJvmThreadCount", Thread.activeCount());
        audit.put("usedHeapMb", usedMemoryMb);
        audit.put("maxHeapMb", maxMemoryMb);
        audit.put("virtualThreadsEnabled", virtualThreadsEnabled);
        return ResponseEntity.ok(audit);
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

        metrics.put("totalConnectionsEver",
                webSocketHandler.getTotalConnectionsEver());

        metrics.put("totalMessagesReceived",
                webSocketHandler.getTotalMessagesReceived());

        metrics.put("failedConnections",
                webSocketHandler.getFailedConnections());

        metrics.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(metrics);
    }
}