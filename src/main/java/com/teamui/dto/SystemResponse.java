package com.teamui.dto;

import com.teamui.domain.enums.Criticality;

import java.time.Instant;
import java.util.UUID;

/**
 * IT system payload returned by the API.
 *
 * @param id          system UUID
 * @param name        system name
 * @param description what it does
 * @param criticality business impact level
 * @param teamId      owning team
 * @param teamName    team name
 * @param createdAt   when the system was registered
 * @author TeamUI
 * @since 0.0.1
 */
public record SystemResponse(
        UUID id,
        String name,
        String description,
        Criticality criticality,
        UUID teamId,
        String teamName,
        Instant createdAt
) {}
