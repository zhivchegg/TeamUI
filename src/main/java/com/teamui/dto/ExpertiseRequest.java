package com.teamui.dto;

import com.teamui.domain.enums.ExpertiseLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to assign or update a user's expertise level on a system.
 *
 * @param userId  the employee
 * @param level   EXPERT / ADVANCED / BASIC / NONE
 * @author TeamUI
 * @since 0.0.1
 */
public record ExpertiseRequest(
        @NotNull UUID userId,
        @NotNull ExpertiseLevel level
) {}
