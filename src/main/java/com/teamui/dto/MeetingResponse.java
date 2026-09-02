package com.teamui.dto;

import com.teamui.domain.enums.MeetingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full 1:1 meeting payload returned by the API.
 *
 * <p>{@code sharedNotes} are visible to both leads of the team.
 * {@code privateNotes} are visible only to the meeting author ({@code leadId}).</p>
 *
 * @param id              meeting UUID
 * @param participantId   participant user id
 * @param participantName participant full name
 * @param leadId          author (lead) user id
 * @param leadName        author full name
 * @param teamId          owning team id
 * @param scheduledDate   when the meeting is/was scheduled
 * @param status          lifecycle state
 * @param energyScore     optional 1-5 score
 * @param energyNote      optional description
 * @param sharedNotes     notes visible to both leads (may be null if caller has no right)
 * @param privateNotes    notes visible only to author (may be null if caller has no right)
 * @param actions         action items created during this meeting
 * @param version         optimistic-lock version
 * @param createdAt       when the record was created
 * @param updatedAt       last modification timestamp
 * @author TeamUI
 * @since 0.0.1
 */
public record MeetingResponse(
        UUID id,
        UUID participantId,
        String participantName,
        UUID leadId,
        String leadName,
        UUID teamId,
        Instant scheduledDate,
        MeetingStatus status,
        Short energyScore,
        String energyNote,
        String sharedNotes,
        String privateNotes,
        List<ActionResponse> actions,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
