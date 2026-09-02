package com.teamui.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Historical snapshot of a competency score.
 *
 * @param id             history record UUID
 * @param userId         scored employee
 * @param userName       employee full name
 * @param competencyId   competency axis
 * @param competencyName competency name
 * @param selfScore      self-rating at the time of snapshot
 * @param leadScore      lead rating at the time of snapshot
 * @param scoredById     who made the last change
 * @param scoredByName   full name of scorer
 * @param comment        optional feedback
 * @param scoredAt       when the snapshot was taken
 * @author TeamUI
 * @since 0.0.1
 */
public record CompetencyScoreHistoryResponse(
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
