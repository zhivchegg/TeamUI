package com.teamui.dto;

import com.teamui.domain.enums.EventType;
import com.teamui.domain.enums.ImpactLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Full timeline event payload returned by the API.
 *
 * @param id                event UUID
 * @param userId            employee this event is about
 * @param userName          full name of the employee
 * @param createdById       who recorded the event
 * @param createdByName     full name of the recorder
 * @param teamId            owning team
 * @param teamName          team name
 * @param eventType         type of event
 * @param category          optional sub-category
 * @param title             headline
 * @param description       details
 * @param impactLevel       magnitude / direction
 * @param visibleToEmployee whether the employee sees this
 * @param createdAt         when the event was recorded
 * @author TeamUI
 * @since 0.0.1
 */
public record EventResponse(
        UUID id,
        UUID userId,
        String userName,
        UUID createdById,
        String createdByName,
        UUID teamId,
        String teamName,
        EventType eventType,
        String category,
        String title,
        String description,
        ImpactLevel impactLevel,
        boolean visibleToEmployee,
        Instant createdAt
) {}
