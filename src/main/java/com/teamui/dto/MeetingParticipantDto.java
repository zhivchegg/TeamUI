package com.teamui.dto;

import com.teamui.domain.enums.MeetingLevel;

import java.util.UUID;

/**
 * A person the caller can schedule a 1:1 with, and the level such a meeting would get.
 *
 * @param id        participant user UUID
 * @param fullName  participant full name
 * @param level     level of the meeting: TEAM, CLUSTER or STREAM
 * @param scopeName team name for TEAM and CLUSTER levels, cluster name for STREAM level
 * @author TeamUI
 * @since 0.0.1
 */
public record MeetingParticipantDto(
        UUID id,
        String fullName,
        MeetingLevel level,
        String scopeName
) {}
