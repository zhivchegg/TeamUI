package com.teamui.dto;

import com.teamui.domain.enums.MeetingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request to update a meeting's core data (not notes).
 *
 * @param scheduledDate new scheduled date
 * @param status          updated status
 * @param energyScore     1-5 scale (optional)
 * @param energyNote      free-text description of energy level
 * @param version         current optimistic-lock version
 * @author TeamUI
 * @since 0.0.1
 */
public record UpdateMeetingRequest(
        @NotNull Instant scheduledDate,
        @NotNull MeetingStatus status,
        @Min(1) @Max(5) Short energyScore,
        String energyNote,
        @NotNull Long version
) {}
