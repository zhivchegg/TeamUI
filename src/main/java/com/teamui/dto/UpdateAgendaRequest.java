package com.teamui.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Participant's agenda for a 1:1 meeting.
 *
 * @param agenda  questions and topics to discuss, null or blank to clear
 * @param version optimistic-lock version of the meeting
 * @author TeamUI
 * @since 0.0.1
 */
public record UpdateAgendaRequest(
        String agenda,
        @NotNull Long version
) {}
