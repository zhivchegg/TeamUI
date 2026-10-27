package com.teamui.exception;

/**
 * Thrown when a resource addressed by the URL does not exist. Mapped to HTTP 404.
 *
 * <p>An id sent in the request body that does not exist is a bad request and stays
 * {@link IllegalArgumentException} (HTTP 400).</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
