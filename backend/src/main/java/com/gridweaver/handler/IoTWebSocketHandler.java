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

    // ConcurrentHashMap: thread-safe session store
    // Industry standard for managing WebSocket sessions
    private final ConcurrentHashMap<String, WebSocketSession>
        activeSessions = new ConcurrentHashMap<>();

    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalConnectionsEver  = new AtomicLong(0);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSessions.put(session.getId(), session);
        totalConnectionsEver.incrementAndGet();
        log.info("[CONNECT] id={} | active={} | virtualThread={}",
            session.getId(),
            activeSessions.size(),
            Thread.currentThread().isVirtual());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws IOException {
        totalMessagesReceived.incrementAndGet();

        // Build ACK response
        String ack = String.format(
            "{\"ack\":true,\"sessionId\":\"%s\",\"receivedAt\":%d}",
            session.getId(),
            System.currentTimeMillis()
        );

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(ack));
        }

        log.debug("[MSG] session={} payload={} totalMsgs={}",
            session.getId(),
            message.getPayload(),
            totalMessagesReceived.get());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {
        activeSessions.remove(session.getId());
        log.info("[DISCONNECT] id={} | remaining={}",
            session.getId(), activeSessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session,
                                     Throwable exception) {
        log.error("[ERROR] session={} error={}",
            session.getId(), exception.getMessage());
        activeSessions.remove(session.getId());
    }

    // ── Metrics (used by controller) ─────────────────

    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    public long getTotalMessagesReceived() {
        return totalMessagesReceived.get();
    }

    public long getTotalConnectionsEver() {
        return totalConnectionsEver.get();
    }
}