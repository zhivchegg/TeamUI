package com.teamui.domain.enums;

/**
 * Types of events recorded on an employee's timeline.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum EventType {

    /** Positive contribution or significant achievement. */
    ACHIEVEMENT,

    /** Free-form note for context during 1:1s. */
    NOTE,

    /** Training completed or initiated. */
    TRAINING,

    /** Incident participated in or owned. */
    INCIDENT,

    /** Negative event: late arrival. */
    LATENESS,

    /** Negative event: missed assigned task or deadline. */
    TASK_MISS
}
