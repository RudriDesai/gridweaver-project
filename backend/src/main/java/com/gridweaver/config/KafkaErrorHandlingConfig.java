package com.gridweaver.config;

import com.gridweaver.kafka.consumer.TelemetryConsumerService;
import com.gridweaver.kafka.dto.TelemetryEvent;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Phase B13: consumer reliability.
 * Failed telemetry records are retried twice (1s apart) before being
 * routed to "telemetry-events.DLT" for later inspection/replay.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    @Value("${gridweaver.kafka.topic.telemetry-events}")
    private String telemetryTopic;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, TelemetryEvent> kafkaTemplate,
                                                 TelemetryConsumerService telemetryConsumerService) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(telemetryTopic + ".DLT", record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, ex) -> {
            telemetryConsumerService.recordDlqEvent();
            log.error("[DLQ] Record sent to dead-letter topic: partition={} offset={} error={}",
                    record.partition(), record.offset(), ex.getMessage());
            recoverer.accept(record, ex);
        }, new FixedBackOff(1000L, 2));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            telemetryConsumerService.recordRetry();
            log.warn("[RETRY] delivery attempt={} error={}", deliveryAttempt, ex.getMessage());
        });

        return errorHandler;
    }
}