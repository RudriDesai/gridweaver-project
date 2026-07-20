package com.gridweaver.kafka.consumer;

import com.gridweaver.kafka.dto.TelemetryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class TelemetryConsumerService {

    private final AtomicLong consumedCount = new AtomicLong(0);
    private final AtomicReference<TelemetryEvent> lastEvent = new AtomicReference<>();

    @KafkaListener(topics = "telemetry-events", groupId = "gridweaver-consumer-group")
    public void consume(TelemetryEvent event) {
        consumedCount.incrementAndGet();
        lastEvent.set(event);
        log.info("Consumed telemetry for node {} (zone {})", event.nodeId(), event.zoneId());
        // Day 2 (Phase B11) will route this into the State Machine
    }

    public long getConsumedCount() {
        return consumedCount.get();
    }

    public TelemetryEvent getLastEvent() {
        return lastEvent.get();
    }
}