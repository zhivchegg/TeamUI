package com.teamui.service;

import com.teamui.domain.cluster.Cluster;
import com.teamui.domain.cluster.ClusterRepository;
import com.teamui.domain.enums.MeetingLevel;
import com.teamui.domain.enums.UserRole;
import com.teamui.domain.membership.TeamMembership;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.team.Team;
import com.teamui.domain.team.TeamRepository;
import com.teamui.domain.user.User;
import com.teamui.dto.MeetingParticipantDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists the people a lead can schedule a 1:1 with.
 *
 * <p>The rules mirror {@link MeetingService}: a lead meets team members of the teams they lead
 * (TEAM level), team leads of the teams in the clusters they lead (CLUSTER level), and cluster
 * leads of the clusters in the streams they lead (STREAM level). A person reachable at several
 * levels is listed once, at the first level in that order, which is the level the meeting gets.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class MeetingParticipantService {

    /** Roles that lead a team, as team lead or IT lead. */
    private static final List<UserRole> LEAD_ROLES = List.of(UserRole.TEAM_LEAD, UserRole.IT_LEAD);

    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final ClusterRepository clusterRepository;
    private final LeadershipResolver leadershipResolver;

    public MeetingParticipantService(TeamMembershipRepository teamMembershipRepository,
                                     TeamRepository teamRepository,
                                     ClusterRepository clusterRepository,
                                     LeadershipResolver leadershipResolver) {
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.clusterRepository = clusterRepository;
        this.leadershipResolver = leadershipResolver;
    }

    /**
     * People the lead can schedule a 1:1 with. The lead is never in the list.
     *
     * @param leadId the authenticated lead
     * @return one entry per person, in level order TEAM, CLUSTER, STREAM
     */
    @Transactional(readOnly = true)
    public List<MeetingParticipantDto> listParticipants(UUID leadId) {
        Map<UUID, MeetingParticipantDto> byId = new LinkedHashMap<>();
        addTeamMembers(leadId, byId);
        addTeamLeads(leadId, byId);
        addClusterLeads(leadId, byId);
        return List.copyOf(byId.values());
    }

    /** TEAM: members of the teams this lead leads. */
    private void addTeamMembers(UUID leadId, Map<UUID, MeetingParticipantDto> byId) {
        for (TeamMembership led : teamMembershipRepository.findAllByUserIdAndRoleIn(leadId, LEAD_ROLES)) {
            Team team = led.getTeam();
            for (TeamMembership member : teamMembershipRepository
                    .findAllByTeamIdAndRoleIn(team.getId(), List.of(UserRole.TEAM_MEMBER))) {
                add(byId, leadId, member.getUser(), MeetingLevel.TEAM, team.getName());
            }
        }
    }

    /** CLUSTER: team leads of the teams in the clusters this lead leads. */
    private void addTeamLeads(UUID leadId, Map<UUID, MeetingParticipantDto> byId) {
        for (Cluster cluster : leadershipResolver.clustersLedBy(leadId)) {
            for (Team team : teamRepository.findAllByClusterId(cluster.getId())) {
                for (TeamMembership lead : teamMembershipRepository.findAllByTeamIdAndRoleIn(team.getId(), LEAD_ROLES)) {
                    add(byId, leadId, lead.getUser(), MeetingLevel.CLUSTER, team.getName());
                }
            }
        }
    }

    /** STREAM: cluster leads and cluster IT leads of the clusters in the streams this lead leads. */
    private void addClusterLeads(UUID leadId, Map<UUID, MeetingParticipantDto> byId) {
        for (UUID streamId : leadershipResolver.ledStreamIds(leadId)) {
            for (Cluster cluster : clusterRepository.findAllByStreamId(streamId)) {
                User clusterLead = cluster.getClusterLead();
                if (clusterLead != null) {
                    add(byId, leadId, clusterLead, MeetingLevel.STREAM, cluster.getName());
                }
                User clusterItLead = cluster.getClusterItLead();
                if (clusterItLead != null) {
                    add(byId, leadId, clusterItLead, MeetingLevel.STREAM, cluster.getName());
                }
            }
        }
    }

    private void add(Map<UUID, MeetingParticipantDto> byId, UUID leadId, User user, MeetingLevel level, String scopeName) {
        if (user.getId().equals(leadId)) {
            return;
        }
        byId.putIfAbsent(user.getId(),
                new MeetingParticipantDto(user.getId(), user.getFirstName() + " " + user.getLastName(), level, scopeName));
    }
}
