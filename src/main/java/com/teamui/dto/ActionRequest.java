package com.teamui.dto;

import com.teamui.domain.enums.ActionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to create or update an action item.
 *
 * @param text    description (non-empty)
 * @param ownerId user who owns the action
 * @param dueDate optional deadline
 * @param status  current status
 * @author TeamUI
 * @since 0.0.1
 */
public record ActionRequest(
        @NotBlank String text,
        @NotNull UUID ownerId,
        LocalDate dueDate,
        ActionStatus status
) {}
