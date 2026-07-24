package com.gridweaver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gridweaver.simulator.IoTSimulatorClient.SimulationStatus;
import com.gridweaver.simulator.SimulatorService;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    /**
     * Starts a simulation batch. Example:
     * POST /api/simulator/start?nodeCount=1000&messagesPerNode=3
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestParam(defaultValue = "100") int nodeCount,
            @RequestParam(defaultValue = "3") int messagesPerNode) {

        Map<String, Object> response = new HashMap<>();

        if (simulatorService.isRunning()) {
            response.put("error", "Simulation already running");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            simulatorService.start(nodeCount, messagesPerNode);
            response.put("message", "Simulation started");
            response.put("nodeCount", nodeCount);
            response.put("messagesPerNode", messagesPerNode);
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Polls current simulation progress.
     * GET /api/simulator/status
     */
    @GetMapping("/status")
    public ResponseEntity<SimulationStatus> status() {
        return ResponseEntity.ok(simulatorService.getStatus());
    }
    
    @PostMapping("/storm")
    public ResponseEntity<String> triggerStorm(@RequestParam(defaultValue = "50") int nodeCount) {
        simulatorService.triggerStormScenario(nodeCount);
        return ResponseEntity.ok("Storm scenario triggered for " + nodeCount + " nodes");
    }
    
    /**
     * Phase A14: pure producer throughput validation, no WS overhead.
     * POST /api/simulator/stress-test?nodeCount=5000&messagesPerNode=5
     */
    @PostMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(
            @RequestParam(defaultValue = "1000") int nodeCount,
            @RequestParam(defaultValue = "5") int messagesPerNode) {

        Map<String, Object> response = new HashMap<>();
        if (simulatorService.isRunning()) {
            response.put("error", "Simulation already running");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        try {
            simulatorService.startStressTest(nodeCount, messagesPerNode);
            response.put("message", "Producer stress test started");
            response.put("nodeCount", nodeCount);
            response.put("messagesPerNode", messagesPerNode);
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}