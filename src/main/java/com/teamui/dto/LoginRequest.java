package com.teamui.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for user login requests.
 *
 * @param email    the user's registered email
 * @param password the plain-text password (transferred over HTTPS only)
 * @author TeamUI
 * @since 0.0.1
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
