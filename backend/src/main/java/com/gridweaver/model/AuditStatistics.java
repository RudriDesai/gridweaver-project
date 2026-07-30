package com.gridweaver.model;

import java.util.Map;

public record AuditStatistics(
        long totalEvents,
        double eventsPerHour,
        Map<String, Long> stateTransitionCounts,  // "FROM->TO" -> count
        long faultTransitionCount,
        Map<String, Long> zoneActivityCounts,      // zoneId -> event count
        long windowMs,
        long timestamp
) {}