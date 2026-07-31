package com.gridweaver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.handler.IoTWebSocketHandler;
import com.gridweaver.model.AuditEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Optimized from synchronous per-event broadcast to buffered
 * batch broadcast.
 *
 * Registers as an EventAuditService listener. Instead of broadcasting every
 * audit event immediately, events are queued and flushed in batches every
 * 500ms. This reduces WebSocket traffic while keeping near real-time updates.
 *
 * Backward compatibility is maintained because the frontend supports both
 * the legacy AUDIT_EVENT message and the new AUDIT_EVENT_BATCH message.
 */
@Component
public class AuditEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(AuditEventBroadcaster.class);

    /** Maximum number of events sent in one WebSocket message. */
    private static final int MAX_BATCH_SIZE = 200;

    private final EventAuditService eventAuditService;
    private final IoTWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Phase B19 — Non-blocking queue for pending audit events. */
    private final Queue<AuditEvent> pendingEvents = new ConcurrentLinkedQueue<>();

    public AuditEventBroadcaster(EventAuditService eventAuditService,
                                 IoTWebSocketHandler webSocketHandler) {
        this.eventAuditService = eventAuditService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Register a listener that simply enqueues incoming events.
     */
    @PostConstruct
    public void registerListener() {
        eventAuditService.addListener(pendingEvents::offer);
    }

    /**
     * Flush queued audit events every 500ms as a single batch.
     */
    @Scheduled(fixedRate = 500)
    public void flushBatch() {

        if (pendingEvents.isEmpty() ||
                webSocketHandler.getActiveConnectionCount() == 0) {
            return;
        }

        List<AuditEvent> batch = new ArrayList<>();

        AuditEvent event;
        while (batch.size() < MAX_BATCH_SIZE &&
                (event = pendingEvents.poll()) != null) {
            batch.add(event);
        }

        if (batch.isEmpty()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(
                    new AuditEventBatchMessage("AUDIT_EVENT_BATCH", batch)
            );

            webSocketHandler.broadcastToAll(payload);

        } catch (Exception ex) {
            log.warn("[AUDIT-BROADCAST-ERROR] {}", ex.getMessage());
        }
    }

    /**
     * Used by the Audit Health endpoint to monitor queue backlog.
     */
    public int getPendingQueueSize() {
        return pendingEvents.size();
    }

    /**
     * WebSocket payload sent to the frontend.
     */
    private record AuditEventBatchMessage(
            String type,
            List<AuditEvent> events
    ) {
    }
}