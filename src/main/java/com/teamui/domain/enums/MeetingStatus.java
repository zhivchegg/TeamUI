package com.teamui.domain.enums;

/**
 * Lifecycle states of a 1:1 meeting.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum MeetingStatus {

    /** Meeting is scheduled but not yet held. */
    PLANNED,

    /** Meeting has been completed and notes are available. */
    COMPLETED,

    /** Meeting was cancelled (no show by either party). */
    CANCELLED,

    /** Participant did not show up for the scheduled meeting. */
    NO_SHOW
}
