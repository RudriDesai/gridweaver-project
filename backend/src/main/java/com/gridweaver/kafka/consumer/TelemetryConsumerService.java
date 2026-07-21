package com.gridweaver.kafka.consumer;

import com.gridweaver.kafka.dto.TelemetryEvent;
import com.gridweaver.service.GridNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryConsumerService {

    private final GridNodeService gridNodeService;

    private final AtomicLong consumedCount = new AtomicLong(0);
    private final AtomicReference<TelemetryEvent> lastEvent = new AtomicReference<>();

    private final ConcurrentHashMap<String, TelemetryEvent> lastEventByNode =
            new ConcurrentHashMap<>();

    @KafkaListener(topics = "telemetry-events", groupId = "gridweaver-consumer-group")
    public void consume(TelemetryEvent event) {
        consumedCount.incrementAndGet();
        lastEvent.set(event);
        lastEventByNode.put(event.nodeId(), event);

        try {
            // generation acts as the telemetry-driven "power output" signal
            // that GridNodeService/BatteryStateService use to evaluate load
            // and fire state-machine transitions. Consumption/zoneId are
            // carried through for Phase A12 regional analytics.
            gridNodeService.applyTelemetry(event.nodeId(), event.generation());

            log.info("[KAFKA-CONSUME] node={} zone={} battery={} -> state evaluated",
                    event.nodeId(), event.zoneId(), event.batteryState());

        } catch (Exception ex) {
            log.error("[KAFKA-CONSUME-ERROR] node={} : {}", event.nodeId(), ex.getMessage());
        }
        // Any resulting state change is picked up automatically by
        // LiveUpdateBroadcaster (already listening on BatteryStateService)
        // and pushed to the frontend over the existing WebSocket batch.
    }

    public long getConsumedCount() { return consumedCount.get(); }
    public TelemetryEvent getLastEvent() { return lastEvent.get(); }
    public TelemetryEvent getLastEventForNode(String nodeId) { return lastEventByNode.get(nodeId); }
}