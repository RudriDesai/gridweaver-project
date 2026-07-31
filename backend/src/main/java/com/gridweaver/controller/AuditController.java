package com.gridweaver.controller;

import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.AuditEvent;
import com.gridweaver.model.AuditHealthStatus;
import com.gridweaver.model.AuditPageResponse;
import com.gridweaver.model.AuditStatistics;
import com.gridweaver.service.AuditEventBroadcaster;
import com.gridweaver.service.EventAuditService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
public class AuditController {

    private final EventAuditService eventAuditService;
    private final AuditEventBroadcaster auditEventBroadcaster; // Phase B19
    private final IoTWebSocketHandler webSocketHandler;        // Phase B19

    public AuditController(EventAuditService eventAuditService,
                           AuditEventBroadcaster auditEventBroadcaster,
                           IoTWebSocketHandler webSocketHandler) {
        this.eventAuditService = eventAuditService;
        this.auditEventBroadcaster = auditEventBroadcaster;
        this.webSocketHandler = webSocketHandler;
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

    @GetMapping("/statistics")
    public AuditStatistics getStatistics() {
        return eventAuditService.computeStatistics();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String zoneId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        List<AuditEvent> events = eventAuditService.exportEvents(nodeId, zoneId, state, from, to);

        StringBuilder csv = new StringBuilder("eventId,nodeId,zoneId,previousState,newState,reason,timestamp\n");
        for (AuditEvent e : events) {
            csv.append(csvEscape(e.eventId())).append(',')
                    .append(csvEscape(e.nodeId())).append(',')
                    .append(csvEscape(e.zoneId())).append(',')
                    .append(csvEscape(e.previousState())).append(',')
                    .append(csvEscape(e.newState())).append(',')
                    .append(csvEscape(e.reason())).append(',')
                    .append(e.timestamp()).append('\n');
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_events.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    // health/status endpoint for the monitoring panel
    @GetMapping("/health")
    public AuditHealthStatus getHealth() {
        AuditStatistics stats = eventAuditService.computeStatistics();
        int pending = auditEventBroadcaster.getPendingQueueSize();

        // DEGRADED if the broadcast queue is backing up faster than it's
        // being flushed — a simple, cheap early-warning signal.
        String status = pending > 500 ? "DEGRADED" : "UP";

        return new AuditHealthStatus(
                status,
                eventAuditService.getEventCount(),
                stats.eventsPerHour(),
                webSocketHandler.getActiveConnectionCount(),
                pending,
                System.currentTimeMillis());
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}