package com.teamui.domain.enums;

/**
 * Describes the impact magnitude and direction of an event on the timeline.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum ImpactLevel {

    /** Minor positive impact. */
    POSITIVE_LOW,

    /** Major positive impact (e.g., critical incident resolution). */
    POSITIVE_HIGH,

    /** Minor negative impact. */
    NEGATIVE_LOW,

    /** Major negative impact (e.g., repeated task misses). */
    NEGATIVE_HIGH
}
