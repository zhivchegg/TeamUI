package com.teamui.domain.enums;

/**
 * Business criticality of a system for Bus Factor analysis.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum Criticality {

    /** System failure would cause major business disruption. */
    CRITICAL,

    /** System is important but failover or workaround exists. */
    HIGH,

    /** System supports non-critical operations. */
    MEDIUM,

    /** Low business impact if system becomes unavailable. */
    LOW
}
