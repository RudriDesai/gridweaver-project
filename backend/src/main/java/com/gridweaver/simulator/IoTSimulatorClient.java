package com.gridweaver.simulator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Simulates N concurrent IoT devices connecting to the GridWeaver
 * WebSocket endpoint, each on its own Virtual Thread.
 *
 * Day 4 change: runs asynchronously (non-blocking) and exposes
 * live progress so it can be controlled/monitored via REST.
 */
public class IoTSimulatorClient {

    private static final Logger log = LoggerFactory.getLogger(IoTSimulatorClient.class);

    private final String wsUrl;
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger ackCount = new AtomicInteger(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile int targetNodeCount = 0;
    private volatile long startTime = 0;
    private volatile long endTime = 0;

    public IoTSimulatorClient(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Launches the simulation asynchronously on a Virtual Thread executor.
     * Returns immediately — call getStatus() to poll progress.
     */
    public void startSimulationAsync(int nodeCount, int messagesPerNode) {
        if (running.get()) {
            throw new IllegalStateException("Simulation already running");
        }

        running.set(true);
        connectedCount.set(0);
        failedCount.set(0);
        ackCount.set(0);
        completedCount.set(0);
        targetNodeCount = nodeCount;
        startTime = System.currentTimeMillis();
        endTime = 0;

        // Run the whole batch on its own virtual thread so the
        // calling REST request returns immediately (non-blocking).
        Thread.ofVirtual().start(() -> {
            try {
                runBatch(nodeCount, messagesPerNode);
            } finally {
                running.set(false);
                endTime = System.currentTimeMillis();
            }
        });
    }

    private void runBatch(int nodeCount, int messagesPerNode) {
        log.info("Starting simulation: {} nodes, {} messages each", nodeCount, messagesPerNode);
        CountDownLatch completionLatch = new CountDownLatch(nodeCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < nodeCount; i++) {
                final String nodeId = "SIM-NODE-" + String.format("%05d", i + 1);
                executor.submit(() -> simulateNode(nodeId, messagesPerNode, completionLatch));
            }
            completionLatch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulation interrupted");
        }

        log.info("=== SIMULATION COMPLETE === connected={} failed={} acks={}",
            connectedCount.get(), failedCount.get(), ackCount.get());
    }

    private void simulateNode(String nodeId, int messagesPerNode, CountDownLatch latch) {
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectedCount.incrementAndGet();
                for (int m = 0; m < messagesPerNode; m++) {
                    String payload = String.format(
                        "{\"nodeId\":\"%s\",\"seq\":%d,\"powerOutput\":%.1f,\"timestamp\":%d}",
                        nodeId, m, Math.random() * 100, System.currentTimeMillis()
                    );
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                    Thread.sleep(50);
                }
                session.close(CloseStatus.NORMAL);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                ackCount.incrementAndGet();
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                failedCount.incrementAndGet();
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                completedCount.incrementAndGet();
                latch.countDown();
            }
        };

        try {
            client.execute(handler, wsUrl).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failedCount.incrementAndGet();
            latch.countDown();
        }
    }

    /** Snapshot of current simulation progress — safe to call anytime. */
    public SimulationStatus getStatus() {
        long elapsedMs = running.get()
            ? System.currentTimeMillis() - startTime
            : (endTime > 0 ? endTime - startTime : 0);

        return new SimulationStatus(
            running.get(),
            targetNodeCount,
            connectedCount.get(),
            failedCount.get(),
            ackCount.get(),
            completedCount.get(),
            elapsedMs
        );
    }

    /** Immutable status snapshot returned to REST clients. */
    public record SimulationStatus(
        boolean running,
        int targetNodeCount,
        int connected,
        int failed,
        int acksReceived,
        int completed,
        long elapsedMs
    ) {}
}