package com.teamui.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Create or update a competency in the matrix.
 *
 * @param name        competency name, unique across the matrix
 * @param description short description of the competency
 * @param teamId      team the competency belongs to, null for company-wide
 * @param level1      what a person can do at level 1
 * @param level2      what a person can do at level 2
 * @param level3      what a person can do at level 3
 * @param level4      what a person can do at level 4
 * @author TeamUI
 * @since 0.0.1
 */
public record CompetencyDefinitionRequest(
        @NotBlank String name,
        String description,
        UUID teamId,
        String level1,
        String level2,
        String level3,
        String level4
) {}
