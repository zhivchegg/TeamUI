package com.teamui.dto;

import java.util.UUID;

/**
 * Minimal user identity for the dashboard header.
 *
 * @param id        user UUID
 * @param firstName first name
 * @param lastName  last name
 * @param email     email address
 * @param avatarUrl optional avatar URL
 * @author TeamUI
 * @since 0.0.1
 */
public record UserProfileDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String avatarUrl
) {}
