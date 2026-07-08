package com.gridweaver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridweaver.handler.IoTWebSocketHandler;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class GridNodeController {

    @Value("${gridweaver.cors.allowed-origin}")
    private String allowedOrigin;

    private final IoTWebSocketHandler webSocketHandler;

    public GridNodeController(IoTWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    // Health check — verifies Virtual Threads are active
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "UP");
        response.put("application", "GridWeaver");
        response.put("virtualThreadsEnabled", true);
        response.put("activeWsConnections",
            webSocketHandler.getActiveConnectionCount());
        response.put("totalWsMessages",
            webSocketHandler.getTotalMessagesReceived());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

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
