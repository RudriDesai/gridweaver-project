package com.gridweaver.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import com.gridweaver.kafka.producer.TelemetryProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka/producer")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
@RequiredArgsConstructor
public class KafkaProducerController {

    private final TelemetryProducerService producerService;

    @GetMapping("/status")
public Map<String, Object> status() {
    return Map.of(
            "status", "UP",
            "publishedCount", producerService.getPublishedCount(),
            "failedCount", producerService.getFailedCount(),
            "eventsPerSecond", producerService.getEventsPerSecond()
    );
}
}