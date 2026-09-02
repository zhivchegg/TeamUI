package com.teamui.dto;

import com.teamui.domain.enums.MeetingStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact 1:1 meeting card for dashboard lists.
 *
 * @param id             meeting UUID
 * @param scheduledDate  when the meeting is/was scheduled
 * @param status         planned / completed / cancelled / no_show
 * @param participantName name of the participant
 * @param leadName       name of the lead
 * @param energyScore    optional energy score recorded
 * @author TeamUI
 * @since 0.0.1
 */
public record MeetingPreviewDto(
        UUID id,
        Instant scheduledDate,
        MeetingStatus status,
        String participantName,
        String leadName,
        Short energyScore
) {}
