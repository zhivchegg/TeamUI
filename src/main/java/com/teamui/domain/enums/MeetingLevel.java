package com.teamui.domain.enums;

/**
 * Level of a 1:1 meeting in the organisation chain.
 *
 * <ul>
 *   <li>{@link #STREAM}: Stream Lead with a Cluster Lead of the stream. {@code team} is empty.</li>
 *   <li>{@link #CLUSTER}: Cluster Lead with a Team Lead of the cluster. {@code team} is the participant's team.</li>
 *   <li>{@link #TEAM}: Team Lead with a team member. {@code team} is the team.</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
public enum MeetingLevel {
    TEAM,
    CLUSTER,
    STREAM
}
