package com.teamui.dto;

import java.util.List;

/**
 * TEAM_MEMBER dashboard view: personal metrics and recent activity.
 *
 * @param competencyRadar  7-axis star radar scores (self + lead average)
 * @param lastMeetings     last 3 completed 1:1s
 * @param upcomingMeeting  next scheduled 1:1 or null
 * @param openActions      action items owned by this user
 * @param recentEvents     last 3 timeline events
 * @author TeamUI
 * @since 0.0.1
 */
public record MemberMetricsDto(
        List<CompetencyRadarPointDto> competencyRadar,
        List<MeetingPreviewDto> lastMeetings,
        MeetingPreviewDto upcomingMeeting,
        List<QuickActionDto> openActions,
        List<TimelineEventPreviewDto> recentEvents
) {}
