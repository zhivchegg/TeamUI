package com.teamui.dto;

import java.util.List;

/**
 * TEAM_LEAD / IT_LEAD dashboard view: team-level health and activity.
 *
 * @param teamName              the team name
 * @param upcomingMeetings      1:1s scheduled in the next 7 days
 * @param overdueActions        actions past due date
 * @param memberCards           team member summaries
 * @param busFactorAlerts       systems with low expertise coverage
 * @param teamEnergyAverage     average energy score across last pulse surveys
 * @param unacknowledgedEvents  timeline events awaiting lead acknowledgment
 * @author TeamUI
 * @since 0.0.1
 */
public record TeamHealthDto(
        String teamName,
        List<MeetingPreviewDto> upcomingMeetings,
        List<QuickActionDto> overdueActions,
        List<TeamMemberCardDto> memberCards,
        List<BusFactorAlertDto> busFactorAlerts,
        Double teamEnergyAverage,
        List<TimelineEventPreviewDto> unacknowledgedEvents
) {}
