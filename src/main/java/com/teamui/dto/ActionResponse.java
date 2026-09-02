package com.teamui.dto;

import com.teamui.domain.enums.ActionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Action item payload returned by the API.
 *
 * @param id        action UUID
 * @param meetingId originating meeting id
 * @param text      description of the action
 * @param ownerId   user responsible for completing the action
 * @param ownerName full name of the owner
 * @param dueDate   optional deadline
 * @param status    open / in_progress / done / cancelled
 * @param version   optimistic-lock version
 * @param createdAt when created
 * @param updatedAt last modification timestamp
 * @author TeamUI
 * @since 0.0.1
 */
public record ActionResponse(
        UUID id,
        UUID meetingId,
        String text,
        UUID ownerId,
        String ownerName,
        LocalDate dueDate,
        ActionStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
