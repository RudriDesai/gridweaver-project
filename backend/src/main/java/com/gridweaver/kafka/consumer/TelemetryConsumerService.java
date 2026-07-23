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
import org.springframework.scheduling.annotation.Scheduled;
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryConsumerService {

    private final GridNodeService gridNodeService;

    private final AtomicLong consumedCount = new AtomicLong(0);
    private final AtomicReference<TelemetryEvent> lastEvent = new AtomicReference<>();

    private final ConcurrentHashMap<String, TelemetryEvent> lastEventByNode =
            new ConcurrentHashMap<>();

    private final AtomicLong retryCount = new AtomicLong(0);
    private final AtomicLong dlqCount = new AtomicLong(0);
    private final AtomicLong windowProcessed = new AtomicLong(0);
    private final AtomicLong processingLatencySumMs = new AtomicLong(0);
    private final AtomicLong processingLatencySamples = new AtomicLong(0);
    private volatile double processingRatePerSec = 0.0;
    private volatile double avgProcessingLatencyMs = 0.0;

    @KafkaListener(topics = "telemetry-events", groupId = "gridweaver-consumer-group")
    public void consume(TelemetryEvent event) {
        long startNanos = System.nanoTime();

        consumedCount.incrementAndGet();
        lastEvent.set(event);
        lastEventByNode.put(event.nodeId(), event);

        try {
            gridNodeService.applyTelemetry(event);
            log.info("[KAFKA-CONSUME] node={} zone={} battery={} -> state evaluated",
                    event.nodeId(), event.zoneId(), event.batteryState());
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            processingLatencySumMs.addAndGet(elapsedMs);
            processingLatencySamples.incrementAndGet();
            windowProcessed.incrementAndGet();
        }
        // Exceptions propagate to DefaultErrorHandler (retry -> DLQ), not caught here.
    }

    @Scheduled(fixedRate = 1000)
    public void computeProcessingMetrics() {
        processingRatePerSec = windowProcessed.getAndSet(0);

        long samples = processingLatencySamples.getAndSet(0);
        long sum = processingLatencySumMs.getAndSet(0);

        if (samples > 0) {
            avgProcessingLatencyMs =
                    Math.round(((double) sum / samples) * 100.0) / 100.0;
        }
    }

    public long getConsumedCount() { return consumedCount.get(); }
    public TelemetryEvent getLastEvent() { return lastEvent.get(); }
    public TelemetryEvent getLastEventForNode(String nodeId) { return lastEventByNode.get(nodeId); }
    public void recordRetry() { retryCount.incrementAndGet(); }
    public void recordDlqEvent() { dlqCount.incrementAndGet(); }
    public long getRetryCount() { return retryCount.get(); }
    public long getDlqCount() { return dlqCount.get(); }
    public double getProcessingRatePerSec() { return processingRatePerSec; }
    public double getAvgProcessingLatencyMs() { return avgProcessingLatencyMs; }
}