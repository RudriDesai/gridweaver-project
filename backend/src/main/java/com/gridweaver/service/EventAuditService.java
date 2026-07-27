package com.gridweaver.service;

import com.gridweaver.model.AuditEvent;
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
    private final Deque<AuditEvent> auditLog = new ConcurrentLinkedDeque<>();

    public EventAuditService(BatteryStateService batteryStateService) {
        this.batteryStateService = batteryStateService;
    }

    @PostConstruct
    public void registerAuditListener() {
        batteryStateService.addListener((nodeId, oldState, newState) -> {
            AuditEvent event = AuditEvent.of(
                    nodeId,
                    oldState.name(),
                    newState.name(),
                    deriveReason(oldState.name(), newState.name())
            );

            auditLog.addFirst(event);
            while (auditLog.size() > MAX_AUDIT_HISTORY) {
                auditLog.removeLast();
            }

            log.debug("[AUDIT] {} : {} -> {} ({})", nodeId, oldState, newState, event.reason());
        });
    }

    /**
     * Human-readable reason derived from the transition itself.
     * Kept simple/deterministic for Day 1 — no external classification needed.
     */
    private String deriveReason(String fromState, String toState) {
        if ("FAULT".equals(toState)) return "Sensor reading out of valid range";
        if ("FAULT".equals(fromState)) return "Fault cleared — reading back in range";
        if ("CHARGING".equals(toState)) return "Grid load dropped below charge threshold";
        if ("DISCHARGING".equals(toState)) return "Grid load exceeded discharge threshold";
        return "Grid load returned to normal operating range";
    }

    public List<AuditEvent> getRecentEvents(int limit) {
        return auditLog.stream().limit(limit).collect(Collectors.toList());
    }

    public List<AuditEvent> getAllEvents() {
        return new ArrayList<>(auditLog);
    }

    public int getEventCount() {
        return auditLog.size();
    }
}