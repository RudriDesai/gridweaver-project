package com.gridweaver.simulator;

import org.springframework.stereotype.Service;

import com.gridweaver.kafka.producer.TelemetryProducerService;
import com.gridweaver.service.BatteryStateService;
import com.gridweaver.service.GridNodeService;
import com.gridweaver.kafka.dto.TelemetryEvent;
import java.time.Instant;

/**
 * Spring-managed wrapper around IoTSimulatorClient.
 * Ensures a single simulation instance is controlled application-wide,
 * and provides the REST layer with a stable point of access.
 */
@Service
public class SimulatorService {

    private static final String DEFAULT_WS_URL = "ws://localhost:8080/ws/iot";

    private final IoTSimulatorClient simulatorClient;
    private final GridNodeService gridNodeService;
    private final TelemetryProducerService telemetryProducerService;

    public SimulatorService(GridNodeService gridNodeService,
            TelemetryProducerService telemetryProducerService) {

        this.gridNodeService = gridNodeService;
        this.telemetryProducerService = telemetryProducerService;

        this.simulatorClient = new IoTSimulatorClient(DEFAULT_WS_URL, telemetryProducerService);
    }

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

    /**
     * Simulates a storm event: N nodes simultaneously drop power output,
     * driving them toward DISCHARGING/FAULT states at once. Proves the
     * end-to-end pipeline (state machine -> broadcast -> map) holds up
     * under a realistic mass-event load, as described in the use case.
     */
    public void triggerStormScenario(int affectedNodeCount) {

        if (affectedNodeCount <= 0 || affectedNodeCount > 50_000) {
            throw new IllegalArgumentException(
                    "affectedNodeCount must be between 1 and 50,000");
        }

        Thread.ofVirtual().start(() -> {

            String[] zones = { "ZONE-A", "ZONE-B", "ZONE-C", "ZONE-D" };

            for (int i = 0; i < affectedNodeCount; i++) {

                final String nodeId = "STORM-NODE-" + String.format("%05d", i + 1);

                final String zone = zones[i % zones.length];

                Thread.ofVirtual().start(() -> {

                    try {

                        double generation = 95 + Math.random() * 5;

                        double consumption = 70 + Math.random() * 20;

                        double batteryLevel = Math.random() * 20;

                        // Storm causes nodes to discharge
                        String batteryState = "DISCHARGING";

                        TelemetryEvent event = new TelemetryEvent(
                                nodeId,
                                zone,
                                generation,
                                consumption,
                                batteryLevel,
                                batteryState,
                                Instant.now());

                        telemetryProducerService.publish(event);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
            }
        });
    }
}