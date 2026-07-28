package com.gridweaver.controller;

import com.gridweaver.model.AuditEvent;
import com.gridweaver.model.AuditPageResponse;
import com.gridweaver.service.EventAuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class AuditController {

    private final EventAuditService eventAuditService;

    public AuditController(EventAuditService eventAuditService) {
        this.eventAuditService = eventAuditService;
    }

    @GetMapping("/events")
    public List<AuditEvent> getEvents(@RequestParam(defaultValue = "100") int limit) {
        return eventAuditService.getRecentEvents(limit);
    }

    @GetMapping("/events/query")
    public AuditPageResponse queryEvents(
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String zoneId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return eventAuditService.queryEvents(nodeId, zoneId, state, from, to, page, size);
    }
}