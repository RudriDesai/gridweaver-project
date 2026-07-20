package com.gridweaver.kafka.dto;

import java.time.Instant;

public record TelemetryEvent(
        String nodeId,
        String zoneId,
        double generation,
        double consumption,
        double batteryLevel,
        String batteryState,
        Instant timestamp
) {}