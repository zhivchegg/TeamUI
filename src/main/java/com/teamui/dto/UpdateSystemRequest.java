package com.teamui.dto;

import com.teamui.domain.enums.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update a tracked system.
 *
 * @param name        updated name
 * @param description updated description
 * @param criticality updated criticality
 * @author TeamUI
 * @since 0.0.1
 */
public record UpdateSystemRequest(
        @NotBlank String name,
        String description,
        @NotNull Criticality criticality
) {}
