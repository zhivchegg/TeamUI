package com.teamui.dto;

import java.util.UUID;

/**
 * A competency as shown in the matrix, with the level descriptions and whether the caller may edit it.
 *
 * @param id          competency UUID
 * @param name        competency name
 * @param description short description
 * @param teamId      team the competency belongs to, null for company-wide
 * @param level1      what a person can do at level 1
 * @param level2      what a person can do at level 2
 * @param level3      what a person can do at level 3
 * @param level4      what a person can do at level 4
 * @param canEdit     true if the caller may change or delete the competency
 * @author TeamUI
 * @since 0.0.1
 */
public record CompetencyDefinitionResponse(
        UUID id,
        String name,
        String description,
        UUID teamId,
        String level1,
        String level2,
        String level3,
        String level4,
        boolean canEdit
) {}
