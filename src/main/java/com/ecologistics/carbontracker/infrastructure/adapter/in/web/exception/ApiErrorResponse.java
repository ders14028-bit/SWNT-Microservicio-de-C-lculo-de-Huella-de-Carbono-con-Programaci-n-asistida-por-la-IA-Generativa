package com.ecologistics.carbontracker.infrastructure.adapter.in.web.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato uniforme de error expuesto por la API REST.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public ApiErrorResponse(int status, String error, String message, List<String> details) {
        this(Instant.now(), status, error, message, details);
    }
}
