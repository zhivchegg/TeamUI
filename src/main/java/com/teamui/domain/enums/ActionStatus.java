package com.teamui.domain.enums;

/**
 * Status of an action item created during a 1:1 meeting.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum ActionStatus {

    /** Action item is awaiting start. */
    OPEN,

    /** Action item is actively being worked on. */
    IN_PROGRESS,

    /** Action item has been completed. */
    DONE,

    /** Action item was abandoned without completion. */
    CANCELLED
}
