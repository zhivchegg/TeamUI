package com.teamui.dto;

import java.util.UUID;

/**
 * Minimal team health card shown in the stream aggregate view.
 *
 * @param id              team UUID
 * @param name            team name
 * @param memberCount     active member count
 * @param overdueActions  overdue action items
 * @param avgEnergy       average pulse energy score (1-5) or null
 * @param atRisk          true if overdueActions > 0 or bus factor issues
 * @author TeamUI
 * @since 0.0.1
 */
public record TeamMiniSummaryDto(
        UUID id,
        String name,
        int memberCount,
        long overdueActions,
        Double avgEnergy,
        boolean atRisk
) {}
