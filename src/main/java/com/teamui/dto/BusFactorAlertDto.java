package com.teamui.dto;

import com.teamui.domain.enums.Criticality;

import java.util.List;
import java.util.UUID;

/**
 * Warning for a system with insufficient expert coverage.
 *
 * @param systemId      system UUID
 * @param systemName    system name
 * @param criticality   CRITICAL / HIGH / MEDIUM / LOW
 * @param expertCount   number of EXPERT level owners
 * @param advancedCount number of ADVANCED level owners
 * @param totalMembers  total team members who could cover this
 * @param atRisk        true if expertCount == 0 or (expertCount == 1 && criticality >= HIGH)
 * @param expertNames   names of current experts
 * @author TeamUI
 * @since 0.0.1
 */
public record BusFactorAlertDto(
        UUID systemId,
        String systemName,
        Criticality criticality,
        long expertCount,
        long advancedCount,
        int totalMembers,
        boolean atRisk,
        List<String> expertNames
) {}
