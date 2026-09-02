package com.teamui.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request for a lead to submit an assessment of a team member's competency.
 *
 * @param competencyId the competency being scored
 * @param leadScore    1–5 scale
 * @param comment      optional feedback
 * @author TeamUI
 * @since 0.0.1
 */
public record ScoreLeadRequest(
        @NotNull UUID competencyId,
        @Min(1) @Max(5) Short leadScore,
        String comment
) {}
