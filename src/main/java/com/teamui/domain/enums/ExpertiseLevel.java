package com.teamui.domain.enums;

/**
 * Knowledge depth of an employee regarding a specific system (Bus Factor tracking).
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum ExpertiseLevel {

    /** Can design, troubleshoot, and mentor others on this system. */
    EXPERT,

    /** Can independently operate and troubleshoot common issues. */
    ADVANCED,

    /** Can perform basic operations with guidance. */
    BASIC,

    /** No current knowledge of this system. */
    NONE
}
