package com.gridweaver.controller;

import com.gridweaver.model.ZoneStats;
import com.gridweaver.service.RegionalAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
@RequiredArgsConstructor
public class AnalyticsController {

    private final RegionalAnalyticsService analyticsService;

    @GetMapping("/zones")
    public List<ZoneStats> getZoneStats() {
        return analyticsService.computeZoneStats();
    }
}