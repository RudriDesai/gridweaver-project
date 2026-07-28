package com.gridweaver.service;

import com.gridweaver.model.AuditEvent;
import com.gridweaver.model.AuditPageResponse;
import com.gridweaver.model.GridNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class EventAuditService {

    private static final Logger log = LoggerFactory.getLogger(EventAuditService.class);
    private static final int MAX_AUDIT_HISTORY = 2000;

    private final BatteryStateService batteryStateService;
    private final GridNodeService gridNodeService;
    private final Deque<AuditEvent> auditLog = new ConcurrentLinkedDeque<>();

    public EventAuditService(BatteryStateService batteryStateService, GridNodeService gridNodeService) {
        this.batteryStateService = batteryStateService;
        this.gridNodeService = gridNodeService;
    }

    @PostConstruct
    public void registerAuditListener() {
        batteryStateService.addListener((nodeId, oldState, newState) -> {
            GridNode node = gridNodeService.getNodeById(nodeId);
            String zoneId = node != null ? node.getZoneId() : null;

            AuditEvent event = AuditEvent.of(
                    nodeId, zoneId,
                    oldState.name(), newState.name(),
                    deriveReason(oldState.name(), newState.name()));

            auditLog.addFirst(event);
            while (auditLog.size() > MAX_AUDIT_HISTORY) {
                auditLog.removeLast();
            }
            log.debug("[AUDIT] {} : {} -> {} ({})", nodeId, oldState, newState, event.reason());
        });
    }

    private String deriveReason(String fromState, String toState) {
        if ("FAULT".equals(toState)) return "Sensor reading out of valid range";
        if ("FAULT".equals(fromState)) return "Fault cleared — reading back in range";
        if ("CHARGING".equals(toState)) return "Grid load dropped below charge threshold";
        if ("DISCHARGING".equals(toState)) return "Grid load exceeded discharge threshold";
        return "Grid load returned to normal operating range";
    }

    // ── Day 1 (unchanged) ──────────────────────────────
    public List<AuditEvent> getRecentEvents(int limit) {
        return auditLog.stream().limit(limit).collect(Collectors.toList());
    }

    public int getEventCount() {
        return auditLog.size();
    }

    // ── Phase B16 — filtered + paginated query ─────────
    /**
     * @param nodeId optional exact match
     * @param zoneId optional exact match
     * @param state  optional — matches either previousState or newState
     * @param from   optional epoch-millis lower bound (inclusive)
     * @param to     optional epoch-millis upper bound (inclusive)
     */
    public AuditPageResponse queryEvents(String nodeId, String zoneId, String state,
                                         Long from, Long to, int page, int size) {

        List<AuditEvent> filtered = auditLog.stream()
                .filter(e -> nodeId == null || nodeId.equalsIgnoreCase(e.nodeId()))
                .filter(e -> zoneId == null || zoneId.equalsIgnoreCase(e.zoneId()))
                .filter(e -> state == null
                        || state.equalsIgnoreCase(e.previousState())
                        || state.equalsIgnoreCase(e.newState()))
                .filter(e -> from == null || e.timestamp() >= from)
                .filter(e -> to == null || e.timestamp() <= to)
                .collect(Collectors.toList());

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 500));

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil(totalElements / (double) safeSize);

        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<AuditEvent> pageContent = new ArrayList<>(filtered.subList(fromIndex, toIndex));

        return new AuditPageResponse(pageContent, safePage, safeSize, totalElements, totalPages);
    }
}