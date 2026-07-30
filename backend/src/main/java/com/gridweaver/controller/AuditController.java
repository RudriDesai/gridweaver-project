package com.gridweaver.controller;

import com.gridweaver.model.AuditEvent;
import com.gridweaver.model.AuditPageResponse;
import com.gridweaver.model.AuditStatistics;
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

        List<AuditEvent> events =
                eventAuditService.exportEvents(nodeId, zoneId, state, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("eventId,nodeId,zoneId,previousState,newState,reason,timestamp\n");

        for (AuditEvent e : events) {
            csv.append(csvEscape(e.eventId())).append(',')
                    .append(csvEscape(e.nodeId())).append(',')
                    .append(csvEscape(e.zoneId())).append(',')
                    .append(csvEscape(e.previousState())).append(',')
                    .append(csvEscape(e.newState())).append(',')
                    .append(csvEscape(e.reason())).append(',')
                    .append(e.timestamp())
                    .append('\n');
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=audit_events.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")) {

            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}