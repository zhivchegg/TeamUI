package com.teamui.dto;

import com.teamui.domain.enums.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to register a new system for Bus Factor tracking.
 *
 * @param name          system name
 * @param description   what it does
 * @param criticality   business impact level
 * @param teamId        owning team
 * @author TeamUI
 * @since 0.0.1
 */
public record CreateSystemRequest(
        @NotBlank String name,
        String description,
        @NotNull Criticality criticality,
        @NotNull UUID teamId
) {}
