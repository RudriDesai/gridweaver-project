package com.gridweaver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gridweaver.model.GridNode;
import com.gridweaver.service.GridNodeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class GridNodeController {

    private final GridNodeService gridNodeService;

    public GridNodeController(GridNodeService gridNodeService) {
        this.gridNodeService = gridNodeService;
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

    // IMPORTANT: Put /init/{count} BEFORE /{id}
    // Otherwise /nodes/init/5 will match /{id} instead
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
}