package com.teamui.domain.enums;

/**
 * User roles defining access levels in the TeamUI platform.
 *
 * <p>Roles are scoped to team memberships (with the exception of ADMIN).
 * A user may hold multiple roles across different teams.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum UserRole {

    /** Standard team member with access to their own data. */
    TEAM_MEMBER,

    /** Team Lead: conducts 1:1s, manages achievements, sees team dashboards. */
    TEAM_LEAD,

    /** IT Lead: technical counterpart to Team Lead with same team-level access. */
    IT_LEAD,

    /** Stream Lead: aggregate view only, conducts 1:1s with team leads. */
    STREAM_LEAD,

    /** Stream IT Lead: aggregate view only, technical counterpart to Stream Lead. */
    STREAM_IT_LEAD,

    /** Full platform access including user management and configuration. */
    ADMIN
}
