package com.gridweaver.controller;

import com.gridweaver.model.BalancingEvent;
import com.gridweaver.model.BalancingMetrics;
import com.gridweaver.model.BalancingRecommendation;
import com.gridweaver.service.PowerBalancingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/balancing")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class BalancingController {

    private final PowerBalancingService powerBalancingService;

    public BalancingController(PowerBalancingService powerBalancingService) {
        this.powerBalancingService = powerBalancingService;
    }

    @GetMapping("/recommendations")
    public List<BalancingRecommendation> getRecommendations() {
        return powerBalancingService.computeRecommendations();
    }

    @GetMapping("/executions")
    public List<BalancingEvent> getExecutions(@RequestParam(defaultValue = "50") int limit) {
        return powerBalancingService.getRecentExecutions(limit);
    }

    // Phase A17 — aggregate analytics for the live widgets
    @GetMapping("/metrics")
    public BalancingMetrics getMetrics() {
        return powerBalancingService.computeMetrics();
    }
}