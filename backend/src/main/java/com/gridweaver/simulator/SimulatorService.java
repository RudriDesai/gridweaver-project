package com.gridweaver.simulator;

import org.springframework.stereotype.Service;

/**
 * Spring-managed wrapper around IoTSimulatorClient.
 * Ensures a single simulation instance is controlled application-wide,
 * and provides the REST layer with a stable point of access.
 */
@Service
public class SimulatorService {

    private static final String DEFAULT_WS_URL = "ws://localhost:8080/ws/iot";

    private final IoTSimulatorClient simulatorClient = new IoTSimulatorClient(DEFAULT_WS_URL);

    public void start(int nodeCount, int messagesPerNode) {
        if (nodeCount <= 0 || nodeCount > 50_000) {
            throw new IllegalArgumentException("nodeCount must be between 1 and 50,000");
        }
        if (messagesPerNode <= 0 || messagesPerNode > 100) {
            throw new IllegalArgumentException("messagesPerNode must be between 1 and 100");
        }
        simulatorClient.startSimulationAsync(nodeCount, messagesPerNode);
    }

    public IoTSimulatorClient.SimulationStatus getStatus() {
        return simulatorClient.getStatus();
    }

    public boolean isRunning() {
        return simulatorClient.isRunning();
    }
}