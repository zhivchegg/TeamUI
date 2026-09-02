package com.teamui.dto;

import com.teamui.domain.enums.EventType;
import com.teamui.domain.enums.ImpactLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create a timeline event (achievement, incident, note, etc.).
 *
 * @param userId            the employee this event is about
 * @param teamId            owning team
 * @param eventType         type of event
 * @param category          optional sub-category
 * @param title             short headline
 * @param description       optional details
 * @param impactLevel       magnitude and direction
 * @param visibleToEmployee whether the employee can see this on their timeline
 * @author TeamUI
 * @since 0.0.1
 */
public record CreateEventRequest(
        @NotNull UUID userId,
        @NotNull UUID teamId,
        @NotNull EventType eventType,
        String category,
        @NotBlank String title,
        String description,
        ImpactLevel impactLevel,
        boolean visibleToEmployee
) {}
