package com.teamui.dto;

import java.time.Instant;

/**
 * Standard error response returned by the API.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     error type (e.g. Unauthorized, Bad Request)
 * @param message   human-readable description
 * @param path      the requested URI path
 * @author TeamUI
 * @since 0.0.1
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public ApiErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path);
    }
}
