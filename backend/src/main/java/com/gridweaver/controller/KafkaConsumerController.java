package com.gridweaver.controller;

import com.gridweaver.kafka.consumer.TelemetryConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka/consumer")
@RequiredArgsConstructor
public class KafkaConsumerController {

    private final TelemetryConsumerService consumerService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "UP",
                "consumedCount", consumerService.getConsumedCount(),
                "lastEvent", consumerService.getLastEvent() == null
                        ? "none" : consumerService.getLastEvent()
        );
    }
}