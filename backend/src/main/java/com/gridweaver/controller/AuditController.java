package com.gridweaver.controller;

import com.gridweaver.model.AuditEvent;
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
}