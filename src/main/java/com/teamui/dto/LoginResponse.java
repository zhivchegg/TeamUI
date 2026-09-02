package com.teamui.dto;

import java.util.UUID;

/**
 * Response returned upon successful authentication.
 *
 * @param token     the JWT access token
 * @param tokenType always "Bearer"
 * @param userId    the authenticated user's UUID
 * @param email     the user's email
 * @param role      the user's primary role (without ROLE_ prefix)
 * @param expiresIn token lifetime in seconds
 * @author TeamUI
 * @since 0.0.1
 */
public record LoginResponse(
        String token,
        String tokenType,
        UUID userId,
        String email,
        String role,
        long expiresIn
) {}
