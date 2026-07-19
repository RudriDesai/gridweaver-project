package com.gridweaver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.gridweaver.model.StateTransitionRecord;
import com.gridweaver.service.BatteryStateService;

@RestController
@RequestMapping("/api/states")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class StateHistoryController {

    private final BatteryStateService batteryStateService;

    public StateHistoryController(BatteryStateService batteryStateService) {
        this.batteryStateService = batteryStateService;
    }

    @GetMapping("/history")
    public List<StateTransitionRecord> recentHistory(
            @RequestParam(defaultValue = "50") int limit) {
        return batteryStateService.getRecentHistory(limit);
    }

    @GetMapping("/history/{nodeId}")
    public List<StateTransitionRecord> nodeHistory(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "20") int limit) {
        return batteryStateService.getHistoryForNode(nodeId, limit);
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalEvaluations", batteryStateService.getTotalEvaluations());
        m.put("rejectedTransitions", batteryStateService.getRejectedTransitions());
        m.put("activeStateMachines", batteryStateService.getActiveMachineCount());
        return m;
    }
}