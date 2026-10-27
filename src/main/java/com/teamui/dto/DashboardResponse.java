package com.teamui.dto;

import com.teamui.domain.enums.UserRole;

import java.util.List;
import java.util.UUID;

/**
 * Personalized dashboard payload. Fields are populated based on the user's role.
 *
 * <p>Common fields ({@code userProfile}, {@code quickActions}) are always present.
 * Role-specific sections ({@code memberMetrics}, {@code teamHealth}, {@code streamOverview},
 * {@code clusterOverview}) are populated only when relevant to the current user's view.
 * {@code role} is null when the user has no team, cluster or stream role.</p>
 *
 * @param userProfile     current user identity
 * @param role            resolved role for this session, or null if none is assigned
 * @param quickActions    action items requiring attention (todos)
 * @param memberMetrics   TEAM_MEMBER view: my star-radar, my timeline preview, my 1:1s
 * @param teamHealth      TEAM_LEAD / IT_LEAD view: upcoming meetings, team metrics, bus factor
 * @param streamOverview  STREAM_LEAD / STREAM_IT_LEAD view: aggregate metrics across clusters
 * @param clusterOverview CLUSTER_LEAD / CLUSTER_IT_LEAD view: health of the teams in one cluster
 * @author TeamUI
 * @since 0.0.1
 */
public record DashboardResponse(
        UserProfileDto userProfile,
        UserRole role,
        List<QuickActionDto> quickActions,
        MemberMetricsDto memberMetrics,
        TeamHealthDto teamHealth,
        StreamOverviewDto streamOverview,
        ClusterSummaryDto clusterOverview
) {}
