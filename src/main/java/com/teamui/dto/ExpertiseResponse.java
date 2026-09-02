package com.teamui.dto;

import com.teamui.domain.enums.ExpertiseLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * System-to-user expertise mapping payload.
 *
 * @param id         mapping UUID
 * @param systemId   system UUID
 * @param systemName system name
 * @param userId     employee UUID
 * @param userName   employee full name
 * @param level      EXPERT / ADVANCED / BASIC / NONE
 * @param createdAt  when the expertise was recorded
 * @author TeamUI
 * @since 0.0.1
 */
public record ExpertiseResponse(
        UUID id,
        UUID systemId,
        String systemName,
        UUID userId,
        String userName,
        ExpertiseLevel level,
        Instant createdAt
) {}
