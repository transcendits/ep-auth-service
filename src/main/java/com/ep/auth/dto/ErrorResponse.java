package com.ep.auth.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        String correlationId,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> details
) {
}
