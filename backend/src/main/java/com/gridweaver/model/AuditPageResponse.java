package com.gridweaver.model;

import java.util.List;

public record AuditPageResponse(
        List<AuditEvent> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}