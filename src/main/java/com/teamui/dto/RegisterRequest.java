package com.teamui.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for user self-registration via invitation token.
 *
 * @param token     the invitation token received by email
 * @param firstName the user's first name
 * @param lastName  the user's last name
 * @param password  the plain-text password (min 8 chars)
 * @author TeamUI
 * @since 0.0.1
 */
public record RegisterRequest(

        @NotBlank(message = "Invitation token is required")
        String token,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
