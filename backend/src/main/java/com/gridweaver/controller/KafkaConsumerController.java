package com.gridweaver.controller;

import com.gridweaver.kafka.consumer.TelemetryConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka/consumer")
@CrossOrigin(origins = "${gridweaver.cors.allowed-origin}")
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

    @GetMapping("/last-event/{nodeId}")
    public ResponseEntity<?> lastEventForNode(@PathVariable String nodeId) {
        var event = consumerService.getLastEventForNode(nodeId);
        if (event == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(event);
    }
}