package com.teamui.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full pulse survey payload returned by the API.
 *
 * @param id          survey UUID
 * @param userId      employee UUID
 * @param userName    employee full name
 * @param energyScore 1–5 energy level
 * @param burdenNote  what is draining
 * @param topics      discussion topics
 * @param createdAt   submission timestamp
 * @author TeamUI
 * @since 0.0.1
 */
public record PulseSurveyResponse(
        UUID id,
        UUID userId,
        String userName,
        Short energyScore,
        String burdenNote,
        String topics,
        Instant createdAt
) {}
