package com.teamui.domain.enums;

/**
 * Type of activity represented by external evidence data.
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum EvidenceEventType {

    /** Production incident participation or ownership. */
    INCIDENT,

    /** Code merge request (approved, reviewed, authored). */
    MERGE_REQUEST,

    /** Documentation created or updated. */
    DOCUMENTATION,

    /** Training material consumed or delivered. */
    TRAINING
}
