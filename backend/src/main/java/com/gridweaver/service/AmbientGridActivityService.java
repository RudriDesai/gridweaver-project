package com.gridweaver.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gridweaver.model.GridNode;
import com.gridweaver.simulator.SimulatorService;

/**
 * Root cause of "Audit Monitoring never updates / Kafka Events card
 * stays empty between test runs":
 *
 * Battery-state transitions (which is what feeds EventAuditService,
 * AuditEventBroadcaster and LiveUpdateBroadcaster) are ONLY produced by
 * TelemetryConsumerService reading real telemetry off the "telemetry-events"
 * Kafka topic - and the ONLY thing that publishes to that topic is the
 * WebSocket load simulator. So the moment the simulator finishes (which,
 * now that it correctly stops itself, it does), telemetry stops flowing,
 * no more state transitions happen, and every "live" panel on the
 * dashboard - audit monitoring, the map, zone analytics - freezes at
 * whatever it last saw. That's expected given the current wiring, but
 * it makes the dashboard look broken outside of an active load test.
 *
 * This service gives the 2020 registered grid nodes a small amount of
 * independent, ambient activity so the dashboard stays "alive" even
 * when no simulator run is in progress - matching what the UI already
 * implies (a live monitoring platform, not just a load-test readout).
 *
 * It deliberately goes quiet while the simulator IS running, so it never
 * skews load-test numbers (throughput, Kafka publish counts, etc.).
 */
@Component
public class AmbientGridActivityService {

    private static final Logger log = LoggerFactory.getLogger(AmbientGridActivityService.class);

    /** Fraction of registered nodes nudged on each tick. */
    private static final double SAMPLE_FRACTION = 0.01; // 1%
    private static final int MIN_PER_TICK = 3;
    private static final int MAX_PER_TICK = 40;

    private final GridNodeService gridNodeService;
    private final SimulatorService simulatorService;

    public AmbientGridActivityService(GridNodeService gridNodeService, SimulatorService simulatorService) {
        this.gridNodeService = gridNodeService;
        this.simulatorService = simulatorService;
    }

    @Scheduled(fixedRate = 3000)
    public void nudgeRandomNodes() {

        // Don't compete with an active load test - the simulator already
        // drives plenty of real telemetry through Kafka in that case.
        if (simulatorService.isRunning()) {
            return;
        }

        List<GridNode> nodes = gridNodeService.getAllNodes();
        if (nodes.isEmpty()) {
            return;
        }

        int sampleSize = Math.clamp((long) Math.ceil(nodes.size() * SAMPLE_FRACTION),
                MIN_PER_TICK, MAX_PER_TICK);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < sampleSize; i++) {
            GridNode node = nodes.get(random.nextInt(nodes.size()));

            // Small drift around the node's current power output, clamped
            // to a plausible 0-10 range, rather than a fully random jump -
            // this keeps ambient movement gentle rather than jittery.
            double drift = (random.nextDouble() - 0.5) * 2.0; // +/-1.0
            double nextPower = Math.max(0.0, Math.min(10.0, node.getPowerOutput() + drift));

            gridNodeService.applyTelemetry(node.getNodeId(), nextPower);
        }

        log.debug("[AMBIENT] nudged {} nodes", sampleSize);
    }
}
