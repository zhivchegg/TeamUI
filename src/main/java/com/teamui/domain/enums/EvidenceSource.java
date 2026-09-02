package com.teamui.domain.enums;

/**
 * External system that produced evidence data.
 *
 * <p>Intended for future integration with Git, Jira, Slack, and alerting tools.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum EvidenceSource {

    /** Git commit or merge request data. */
    GIT,

    /** Jira ticket or sprint activity. */
    JIRA,

    /** Slack participation or reactions. */
    SLACK,

    /** Alertmanager incident or alert acknowledgment. */
    ALERTMANAGER,

    /** Manually added evidence by a team lead. */
    MANUAL
}
