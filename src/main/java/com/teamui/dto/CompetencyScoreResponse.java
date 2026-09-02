package com.teamui.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full competency score payload returned by the API.
 *
 * @param id             score UUID
 * @param userId         scored employee
 * @param userName       employee full name
 * @param competencyId   competency axis
 * @param competencyName competency name
 * @param selfScore      employee self-rating (1–5) or null
 * @param leadScore      lead rating (1–5) or null
 * @param scoredById     lead who provided the leadScore
 * @param scoredByName   lead full name
 * @param comment        optional feedback
 * @param scoredAt       when the lead score was last updated
 * @author TeamUI
 * @since 0.0.1
 */
public record CompetencyScoreResponse(
        UUID id,
        UUID userId,
        String userName,
        UUID competencyId,
        String competencyName,
        Short selfScore,
        Short leadScore,
        UUID scoredById,
        String scoredByName,
        String comment,
        Instant scoredAt
) {}
