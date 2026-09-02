package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated health summary for a single cluster within a stream.
 *
 * @param id                cluster UUID
 * @param name              cluster name
 * @param teamCount         number of teams in this cluster
 * @param memberCount       total members across teams
 * @param overdueActions    overdue action items across all teams
 * @param busFactorWarnings number of systems at risk
 * @param teamSummaries     per-team mini summaries
 * @author TeamUI
 * @since 0.0.1
 */
public record ClusterSummaryDto(
        UUID id,
        String name,
        int teamCount,
        int memberCount,
        long overdueActions,
        long busFactorWarnings,
        List<TeamMiniSummaryDto> teamSummaries
) {}
