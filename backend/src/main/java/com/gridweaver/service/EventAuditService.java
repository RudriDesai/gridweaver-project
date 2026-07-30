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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.gridweaver.model.AuditStatistics;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventAuditService {

    private static final Logger log = LoggerFactory.getLogger(EventAuditService.class);
    private static final int MAX_AUDIT_HISTORY = 2000;
    private static final long STATS_WINDOW_MS = 60 * 60 * 1000L; // 1 hour

    private final BatteryStateService batteryStateService;
    private final GridNodeService gridNodeService;
    private final Deque<AuditEvent> auditLog = new ConcurrentLinkedDeque<>();

    public interface AuditEventListener {
        void onAuditEvent(AuditEvent event);
    }

    private final List<AuditEventListener> listeners = new CopyOnWriteArrayList<>();

    public EventAuditService(BatteryStateService batteryStateService, GridNodeService gridNodeService) {
        this.batteryStateService = batteryStateService;
        this.gridNodeService = gridNodeService;
    }

    public void addListener(AuditEventListener listener) {
        listeners.add(listener);
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

            // Phase B17 — fan out to any real-time listeners (e.g. WebSocket broadcaster)
            for (AuditEventListener l : listeners) {
                try {
                    l.onAuditEvent(event);
                } catch (Exception e) {
                    log.warn("[AUDIT-LISTENER-ERROR] {}", e.getMessage());
                }
            }
        });
    }

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

    public int getEventCount() {
        return auditLog.size();
    }
    private List<AuditEvent> filterEvents(String nodeId,
                                          String zoneId,
                                          String state,
                                          Long from,
                                          Long to) {

        return auditLog.stream()
                .filter(e -> nodeId == null || nodeId.equalsIgnoreCase(e.nodeId()))
                .filter(e -> zoneId == null || zoneId.equalsIgnoreCase(e.zoneId()))
                .filter(e -> state == null
                        || state.equalsIgnoreCase(e.previousState())
                        || state.equalsIgnoreCase(e.newState()))
                .filter(e -> from == null || e.timestamp() >= from)
                .filter(e -> to == null || e.timestamp() <= to)
                .collect(Collectors.toList());
    }

    public AuditPageResponse queryEvents(String nodeId,
                                         String zoneId,
                                         String state,
                                         Long from,
                                         Long to,
                                         int page,
                                         int size) {

        List<AuditEvent> filtered = filterEvents(nodeId, zoneId, state, from, to);

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 500));

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil(totalElements / (double) safeSize);

        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        return new AuditPageResponse(
                new ArrayList<>(filtered.subList(fromIndex, toIndex)),
                safePage,
                safeSize,
                totalElements,
                totalPages
        );
    }
    public List<AuditEvent> exportEvents(String nodeId,
                                         String zoneId,
                                         String state,
                                         Long from,
                                         Long to) {

        return filterEvents(nodeId, zoneId, state, from, to);
    }
    public AuditStatistics computeStatistics() {

        long now = System.currentTimeMillis();
        long windowStart = now - STATS_WINDOW_MS;

        List<AuditEvent> windowEvents = auditLog.stream()
                .filter(e -> e.timestamp() >= windowStart)
                .collect(Collectors.toList());

        Map<String, Long> transitionCounts = new HashMap<>();
        Map<String, Long> zoneCounts = new HashMap<>();

        long faultCount = 0;

        for (AuditEvent event : windowEvents) {

            String transition =
                    event.previousState() + "->" + event.newState();

            transitionCounts.merge(transition, 1L, Long::sum);

            if (event.zoneId() != null) {
                zoneCounts.merge(event.zoneId(), 1L, Long::sum);
            }

            if ("FAULT".equals(event.newState())) {
                faultCount++;
            }
        }

        double hours = STATS_WINDOW_MS / (1000.0 * 60 * 60);

        double eventsPerHour = windowEvents.isEmpty()
                ? 0.0
                : Math.round((windowEvents.size() / hours) * 10.0) / 10.0;

        return new AuditStatistics(
                windowEvents.size(),
                eventsPerHour,
                transitionCounts,
                faultCount,
                zoneCounts,
                STATS_WINDOW_MS,
                now
        );
    }
}