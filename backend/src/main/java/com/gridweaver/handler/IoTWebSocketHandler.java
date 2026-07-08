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
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws IOException {

        totalMessagesReceived.incrementAndGet();

        String ack = String.format(
                "{\"ack\":true,\"sessionId\":\"%s\",\"receivedAt\":%d}",
                session.getId(),
                System.currentTimeMillis()
        );

        try {

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(ack));
            }

        } catch (IOException ex) {

            log.warn(
                    "[SEND-ERROR] session={} error={}",
                    session.getId(),
                    ex.getMessage()
            );

            failedConnections.incrementAndGet();

            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException ignored) {
            }
        }

        log.debug(
                "[MSG] session={} payload={} totalMsgs={}",
                session.getId(),
                message.getPayload(),
                totalMessagesReceived.get()
        );
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