package com.teamui.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request to update meeting notes.
 *
 * <p>Either {@code sharedNotes} or {@code privateNotes} (or both) may be provided.
 * The server enforces visibility rules: only the meeting author may update
 * {@code privateNotes}; either lead of the team may update {@code sharedNotes}.</p>
 *
 * @param sharedNotes  text visible to both leads
 * @param privateNotes text visible only to the author
 * @param version      current optimistic-lock version of the meeting
 * @author TeamUI
 * @since 0.0.1
 */
public record UpdateNotesRequest(
        String sharedNotes,
        String privateNotes,
        @NotNull Long version
) {}
