package com.teamui.dto;

import java.util.List;

/**
 * STREAM_LEAD / STREAM_IT_LEAD aggregate dashboard view.
 *
 * <p>No individual notes or meetings — only high-level health indicators.</p>
 *
 * @param streamName        name of the stream
 * @param clusterSummaries  health per cluster
 * @param totalTeams        total team count across stream
 * @param totalMembers      total headcount
 * @param teamsAtRisk       count of teams with overdue actions or low bus factor
 * @author TeamUI
 * @since 0.0.1
 */
public record StreamOverviewDto(
        String streamName,
        List<ClusterSummaryDto> clusterSummaries,
        int totalTeams,
        int totalMembers,
        int teamsAtRisk
) {}
