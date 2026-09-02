package com.teamui.dto;

import com.teamui.domain.enums.Criticality;

import java.util.List;
import java.util.UUID;

/**
 * System enriched with its expert roster (for the knowledge matrix view).
 *
 * @param id          system UUID
 * @param name        system name
 * @param criticality business impact
 * @param experts     users with EXPERT level
 * @param advanced    users with ADVANCED level
 * @param basic       users with BASIC level
 * @author TeamUI
 * @since 0.0.1
 */
public record SystemWithPeopleDto(
        UUID id,
        String name,
        Criticality criticality,
        List<String> experts,
        List<String> advanced,
        List<String> basic
) {}
