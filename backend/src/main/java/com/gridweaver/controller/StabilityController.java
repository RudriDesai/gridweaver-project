package com.gridweaver.controller;

import com.gridweaver.model.GridStabilityAlert;
import com.gridweaver.model.ZoneStabilityStatus;
import com.gridweaver.service.GridStabilityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stability")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class StabilityController {

    private final GridStabilityService gridStabilityService;

    public StabilityController(GridStabilityService gridStabilityService) {
        this.gridStabilityService = gridStabilityService;
    }

    // Live snapshot — for persistent map highlighting via polling
    @GetMapping("/status")
    public List<ZoneStabilityStatus> getStatus() {
        return gridStabilityService.getCurrentZoneStability();
    }

    // Alert history — for debugging/audit trail of past instability
    @GetMapping("/alerts")
    public List<GridStabilityAlert> getAlerts(@RequestParam(defaultValue = "50") int limit) {
        return gridStabilityService.getRecentAlerts(limit);
    }
}