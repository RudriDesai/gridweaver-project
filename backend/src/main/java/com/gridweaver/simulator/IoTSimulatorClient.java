package com.gridweaver.simulator;

import java.time.Instant;
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

import com.gridweaver.kafka.dto.TelemetryEvent;
import com.gridweaver.kafka.producer.TelemetryProducerService;

/**
 * Simulates N concurrent IoT devices connecting to the GridWeaver
 * WebSocket endpoint, each on its own Virtual Thread.
 *
 * Day 4 change: runs asynchronously (non-blocking) and exposes
 * live progress so it can be controlled/monitored via REST.
 *
 * Day 1 (Phase A10) change: each simulated message is also published
 * to Kafka as a TelemetryEvent, in addition to the existing WebSocket
 * send. Direct WebSocket publishing stays untouched for Day 1 —
 * full replacement (Kafka-only) happens Day 2 per Phase A11.
 */
public class IoTSimulatorClient {

    private static final Logger log = LoggerFactory.getLogger(IoTSimulatorClient.class);
    private static final String[] ZONES = {"ZONE-A", "ZONE-B", "ZONE-C", "ZONE-D"};
    private static final String[] BATTERY_STATES = {"CHARGING", "DISCHARGING", "IDLE", "FULL", "LOW"};

    private final String wsUrl;
    private final TelemetryProducerService telemetryProducerService;

    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger ackCount = new AtomicInteger(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile int targetNodeCount = 0;
    private volatile long startTime = 0;
    private volatile long endTime = 0;

    public IoTSimulatorClient(String wsUrl, TelemetryProducerService telemetryProducerService) {
        this.wsUrl = wsUrl;
        this.telemetryProducerService = telemetryProducerService;
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
                final String zoneId = ZONES[i % ZONES.length];
                executor.submit(() -> simulateNode(nodeId, zoneId, messagesPerNode, completionLatch));
            }
            completionLatch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulation interrupted");
        }

        log.info("=== SIMULATION COMPLETE === connected={} failed={} acks={}",
            connectedCount.get(), failedCount.get(), ackCount.get());
    }

    private void simulateNode(String nodeId, String zoneId, int messagesPerNode, CountDownLatch latch) {
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectedCount.incrementAndGet();
                for (int m = 0; m < messagesPerNode; m++) {
                    double powerOutput = Math.random() * 100;
                    double generation = Math.random() * 100;
                    double consumption = Math.random() * 100;
                    double batteryLevel = Math.random() * 100;
                    String batteryState = BATTERY_STATES[(int) (Math.random() * BATTERY_STATES.length)];

                    String payload = String.format(
                        "{\"nodeId\":\"%s\",\"seq\":%d,\"powerOutput\":%.1f,\"timestamp\":%d}",
                        nodeId, m, powerOutput, System.currentTimeMillis()
                    );
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }

                    // Publish the same telemetry sample to Kafka.
                    // Failures are handled inside the producer service
                    // (logged + counted) and never break the WS loop.
                    try {
                        telemetryProducerService.publish(
                            new TelemetryEvent(nodeId, zoneId, generation,
                                consumption, batteryLevel, batteryState, Instant.now())
                        );
                    } catch (Exception ex) {
                        log.warn("Kafka publish failed for node {}: {}", nodeId, ex.getMessage());
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
    
    /**
     * Phase A14: publishes directly to Kafka on virtual threads with no
     * WebSocket connection per node — isolates and validates raw producer
     * throughput (batching/compression/acks) without WS handshake overhead.
     */
    public void runProducerStressTest(int nodeCount, int messagesPerNode) {
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

        Thread.ofVirtual().start(() -> {
            CountDownLatch latch = new CountDownLatch(nodeCount);
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < nodeCount; i++) {
                    final String nodeId = "STRESS-NODE-" + String.format("%05d", i + 1);
                    final String zoneId = ZONES[i % ZONES.length];
                    executor.submit(() -> {
                        for (int m = 0; m < messagesPerNode; m++) {
                            try {
                                telemetryProducerService.publish(new TelemetryEvent(
                                        nodeId, zoneId,
                                        Math.random() * 100, Math.random() * 100,
                                        Math.random() * 100,
                                        BATTERY_STATES[(int) (Math.random() * BATTERY_STATES.length)],
                                        Instant.now()));
                                ackCount.incrementAndGet();
                            } catch (Exception ex) {
                                failedCount.incrementAndGet();
                            }
                        }
                        completedCount.incrementAndGet();
                        latch.countDown();
                    });
                }
                latch.await(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                endTime = System.currentTimeMillis();
                log.info("=== STRESS TEST COMPLETE === published={} failed={}",
                        ackCount.get(), failedCount.get());
            }
        });
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