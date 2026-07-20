package com.gridweaver.kafka.producer;

import com.gridweaver.kafka.dto.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;

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

    public void publish(TelemetryEvent event) {
        kafkaTemplate.send(topic, event.nodeId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        publishedCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                        log.error("Failed to publish telemetry for node {}: {}",
                                event.nodeId(), ex.getMessage());
                    }
                });
    }

    public long getPublishedCount() {
        return publishedCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }
}