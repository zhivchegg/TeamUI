package com.teamui.dto;

import com.teamui.domain.enums.MeetingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request to schedule a new 1:1 meeting.
 *
 * @param participantId the team member to meet with
 * @param scheduledDate when the meeting is scheduled
 * @param status        usually PLANNED, but may be COMPLETED if back-dating
 * @author TeamUI
 * @since 0.0.1
 */
public record CreateMeetingRequest(
        @NotNull UUID participantId,
        @NotNull Instant scheduledDate,
        MeetingStatus status
) {}
