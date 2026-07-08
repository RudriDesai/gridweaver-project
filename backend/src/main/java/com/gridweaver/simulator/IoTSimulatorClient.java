package com.gridweaver.simulator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
 * Simulates multiple concurrent IoT devices connecting to the GridWeaver
 * WebSocket endpoint using Java 21 Virtual Threads.
 *
 * Each simulated node:
 * 1. Opens a WebSocket connection.
 * 2. Sends a fixed number of telemetry messages.
 * 3. Receives ACKs from the backend.
 * 4. Closes the connection gracefully.
 */
public class IoTSimulatorClient {

    private static final Logger log =
            LoggerFactory.getLogger(IoTSimulatorClient.class);

    // Reuse one WebSocket client for all simulated nodes
    private final StandardWebSocketClient client = new StandardWebSocketClient();

    private final String wsUrl;

    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger ackCount = new AtomicInteger(0);

    public IoTSimulatorClient(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    /**
     * Launches multiple simulated IoT nodes concurrently.
     */
    public void runSimulation(int nodeCount, int messagesPerNode)
            throws InterruptedException {

        log.info("Starting simulation: {} nodes, {} messages each",
                nodeCount, messagesPerNode);

        long startTime = System.currentTimeMillis();

        CountDownLatch completionLatch = new CountDownLatch(nodeCount);

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < nodeCount; i++) {

                final String nodeId =
                        "SIM-NODE-" + String.format("%05d", i + 1);

                executor.submit(() ->
                        simulateNode(nodeId, messagesPerNode, completionLatch));
            }

            boolean completed =
                    completionLatch.await(2, TimeUnit.MINUTES);

            long elapsed = System.currentTimeMillis() - startTime;

            int expectedAcks = nodeCount * messagesPerNode;

            log.info("========== SIMULATION COMPLETE ==========");
            log.info("Requested Nodes : {}", nodeCount);
            log.info("Connected       : {}", connectedCount.get());
            log.info("Failed          : {}", failedCount.get());
            log.info("Expected ACKs   : {}", expectedAcks);
            log.info("ACKs Received   : {}", ackCount.get());
            log.info("Elapsed Time    : {} ms", elapsed);
            log.info("Completed       : {}", completed);
        }
    }

    /**
     * Simulates one IoT device.
     */
    private void simulateNode(String nodeId,
                              int messagesPerNode,
                              CountDownLatch latch) {

        WebSocketHandler handler = new TextWebSocketHandler() {

        	@Override
        	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        	    connectedCount.incrementAndGet();

        	    // Send telemetry messages
        	    for (int m = 0; m < messagesPerNode; m++) {

        	        String payload = String.format(
        	            "{\"nodeId\":\"%s\",\"seq\":%d,\"powerOutput\":%.1f,\"timestamp\":%d}",
        	            nodeId,
        	            m,
        	            Math.random() * 100,
        	            System.currentTimeMillis()
        	        );

        	        if (session.isOpen()) {
        	            session.sendMessage(new TextMessage(payload));
        	        }

        	        // Small gap between messages
        	        Thread.sleep(100);
        	    }

        	    // Give the backend enough time to send ACKs back
        	    Thread.sleep(300);

        	    // Close gracefully
        	    if (session.isOpen()) {
        	        session.close(CloseStatus.NORMAL);
        	    }
        	}

        	@Override
        	protected void handleTextMessage(WebSocketSession session,
        	                                 TextMessage message) {

        	    ackCount.incrementAndGet();

        	    log.debug("[ACK] {} -> {}",
        	            session.getId(),
        	            message.getPayload());
        	}

            @Override
            public void handleTransportError(WebSocketSession session,
                                             Throwable exception) {

                failedCount.incrementAndGet();

                log.warn(
                        "[SIM-ERROR] node={} error={}",
                        nodeId,
                        exception.getMessage()
                );
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session,
                                              CloseStatus status) {

                latch.countDown();
            }
        };

        try {

            client.execute(handler, wsUrl)
                    .get(30, TimeUnit.SECONDS);

        } catch (Exception e) {

            failedCount.incrementAndGet();

            log.warn(
                    "[SIM-CONNECT-FAIL] node={} error={}",
                    nodeId,
                    e.getMessage()
            );

            latch.countDown();
        }
    }
}