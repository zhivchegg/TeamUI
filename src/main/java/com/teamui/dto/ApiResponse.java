package com.teamui.dto;

/**
 * Standard success envelope: {@code { "status": "0", "message": "", "data": ... }}.
 *
 * @param status  "0" for success
 * @param message human-readable message, empty on success
 * @param data    payload
 * @param <T>     payload type
 * @author TeamUI
 * @since 0.0.1
 */
public record ApiResponse<T>(String status, String message, T data) {

    /**
     * Wraps a successful payload.
     *
     * @param data the payload, may be null for empty responses
     * @param <T>  payload type
     * @return envelope with status "0"
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "", data);
    }

    /**
     * Builds an error envelope. The status is the HTTP code as a string, data is null.
     *
     * @param httpStatus HTTP status code, for example 404
     * @param message    text the user can read
     * @return envelope with the code in {@code status}
     */
    public static ApiResponse<Void> error(int httpStatus, String message) {
        return new ApiResponse<>(String.valueOf(httpStatus), message, null);
    }
}
