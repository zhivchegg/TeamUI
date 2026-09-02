package com.teamui.dto;

import com.teamui.domain.enums.EventType;
import com.teamui.domain.enums.ImpactLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update a timeline event.
 *
 * @param eventType         updated type
 * @param category          updated category
 * @param title             updated headline
 * @param description       updated details
 * @param impactLevel       updated impact
 * @param visibleToEmployee updated visibility
 * @author TeamUI
 * @since 0.0.1
 */
public record UpdateEventRequest(
        @NotNull EventType eventType,
        String category,
        @NotBlank String title,
        String description,
        ImpactLevel impactLevel,
        boolean visibleToEmployee
) {}
