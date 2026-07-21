package com.gridweaver.handler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.service.GridNodeService;

@Component
public class IoTWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log =
            LoggerFactory.getLogger(IoTWebSocketHandler.class);

    // Thread-safe storage for active WebSocket sessions
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions =
            new ConcurrentHashMap<>();

 // Metrics
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalConnectionsEver = new AtomicLong(0);
    private final AtomicLong failedConnections = new AtomicLong(0);

    // Services
    private final GridNodeService gridNodeService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public IoTWebSocketHandler(GridNodeService gridNodeService) {
        this.gridNodeService = gridNodeService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        activeSessions.put(session.getId(), session);
        totalConnectionsEver.incrementAndGet();

        log.info(
                "[CONNECT] id={} | active={} | virtualThread={}",
                session.getId(),
                activeSessions.size(),
                Thread.currentThread().isVirtual()
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            totalMessagesReceived.incrementAndGet();

            JsonNode payload = objectMapper.readTree(message.getPayload());
            String nodeId = payload.path("nodeId").asText(null);

            // Phase A11: telemetry no longer mutates state here.
            // The simulator's Kafka publish (TelemetryProducerService) is now
            // the single source of truth; TelemetryConsumerService (Phase B11)
            // is the only caller of gridNodeService.applyTelemetry().
            // This WS channel is kept only for connection/ack metrics.

            String ack = String.format(
                    "{\"ack\":true,\"sessionId\":\"%s\",\"nodeId\":\"%s\"}",
                    session.getId(), nodeId
            );
            session.sendMessage(new TextMessage(ack));

        } catch (Exception e) {
            log.warn("[WS-ERROR] Failed to process telemetry: {}", e.getMessage());
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {

        activeSessions.remove(session.getId());

        log.info(
                "[DISCONNECT] id={} | remaining={} | status={}",
                session.getId(),
                activeSessions.size(),
                status.getCode()
        );
    }

    @Override
    public void handleTransportError(WebSocketSession session,
                                     Throwable exception) {

        failedConnections.incrementAndGet();

        log.warn(
                "[TRANSPORT-ERROR] session={} error={}",
                session.getId(),
                exception.getMessage()
        );

        try {

            if (session.isOpen()) {
                session.close();
            }

        } catch (IOException ignored) {
        }

        activeSessions.remove(session.getId());
    }

    /**
     * Broadcast a message to every connected client.
     */
    public void broadcastToAll(String message) throws IOException {

        for (WebSocketSession session : activeSessions.values()) {

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    public void broadcastPing() throws IOException {
        String ping = String.format(
                "{\"type\":\"PING\",\"activeConnections\":%d,\"ts\":%d}",
                getActiveConnectionCount(), System.currentTimeMillis()
        );
        broadcastToAll(ping);
    }

    // -------------------- Metrics --------------------

    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    public long getTotalMessagesReceived() {
        return totalMessagesReceived.get();
    }

    public long getTotalConnectionsEver() {
        return totalConnectionsEver.get();
    }

    public long getFailedConnections() {
        return failedConnections.get();
    }
}