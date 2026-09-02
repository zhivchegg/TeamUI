package com.teamui.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request for an employee to submit a self-assessment on a competency axis.
 *
 * @param competencyId the competency being scored
 * @param selfScore    1–5 scale
 * @param comment      optional reflection
 * @author TeamUI
 * @since 0.0.1
 */
public record ScoreSelfRequest(
        @NotNull UUID competencyId,
        @Min(1) @Max(5) Short selfScore,
        String comment
) {}
