package com.teamui.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to submit a pre-1:1 pulse check-in.
 *
 * @param energyScore 1–5 energy level
 * @param burdenNote  what is draining the employee most
 * @param topics      discussion topics for the upcoming 1:1
 * @author TeamUI
 * @since 0.0.1
 */
public record SubmitPulseRequest(
        @Min(1) @Max(5) @NotNull Short energyScore,
        String burdenNote,
        String topics
) {}
