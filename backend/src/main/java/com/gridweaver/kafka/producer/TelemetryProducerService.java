package com.gridweaver.kafka.producer;

import com.gridweaver.kafka.dto.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryProducerService {

    private final KafkaTemplate<String, TelemetryEvent> kafkaTemplate;

    @Value("${gridweaver.kafka.topic.telemetry-events}")
    private String topic;

    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    // Phase A11: rolling 1-second window counter for events/sec
    private final AtomicLong windowCount = new AtomicLong(0);
    private volatile double eventsPerSecond = 0.0;

    public void publish(TelemetryEvent event) {
        kafkaTemplate.send(topic, event.nodeId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        publishedCount.incrementAndGet();
                        windowCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                        log.error("Failed to publish telemetry for node {}: {}",
                                event.nodeId(), ex.getMessage());
                    }
                });
    }

    // Phase A11: flips the window counter into an events/sec figure every second.
    @Scheduled(fixedRate = 1000)
    public void computeThroughput() {
        eventsPerSecond = windowCount.getAndSet(0);
    }

    public long getPublishedCount() { return publishedCount.get(); }
    public long getFailedCount() { return failedCount.get(); }
    public double getEventsPerSecond() { return eventsPerSecond; }
}