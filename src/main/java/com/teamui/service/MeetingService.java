package com.teamui.service;

import com.teamui.domain.cluster.Cluster;
import com.teamui.domain.enums.ActionStatus;
import com.teamui.domain.enums.MeetingLevel;
import com.teamui.domain.enums.UserRole;
import com.teamui.domain.meeting.Meeting;
import com.teamui.domain.meeting.MeetingAction;
import com.teamui.domain.meeting.MeetingActionRepository;
import com.teamui.domain.meeting.MeetingRepository;
import com.teamui.domain.membership.TeamMembership;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.team.Team;
import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import com.teamui.dto.*;
import com.teamui.exception.NotFoundException;
import com.teamui.security.service.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for 1:1 meetings: CRUD, notes management with visibility
 * rules, and action-item lifecycle.
 *
 * <p>Visibility:
 * <ul>
 *   <li>{@code sharedNotes} — editable by any lead of the meeting's team;
 *     visible to both leads.</li>
 *   <li>{@code privateNotes} — editable and visible only by the meeting author
 *     ({@code lead}).</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingActionRepository actionRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final LeadershipResolver leadershipResolver;

    /** Roles that count as leading a team when viewing meeting notes. */
    private static final List<UserRole> LEAD_ROLES = List.of(UserRole.TEAM_LEAD, UserRole.IT_LEAD);

    public MeetingService(MeetingRepository meetingRepository,
                           MeetingActionRepository actionRepository,
                           UserRepository userRepository,
                           TeamMembershipRepository teamMembershipRepository,
                           LeadershipResolver leadershipResolver) {
        this.meetingRepository = meetingRepository;
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.leadershipResolver = leadershipResolver;
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /**
     * Schedule a new 1:1 meeting.
     *
     * @param lead    the authenticated lead
     * @param request meeting creation data
     * @return the created meeting
     */
    @Transactional
    public MeetingResponse createMeeting(UserDetailsImpl lead, CreateMeetingRequest request) {
        User participant = userRepository.findById(request.participantId())
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        Scope scope = resolveScope(lead.getId(), participant.getId());

        Meeting meeting = new Meeting();
        meeting.setParticipant(participant);
        meeting.setLead(lead.getUser());
        meeting.setLevel(scope.level());
        meeting.setTeam(scope.team());
        meeting.setCluster(scope.cluster());
        meeting.setScheduledDate(request.scheduledDate());
        meeting.setStatus(request.status() != null ? request.status() : com.teamui.domain.enums.MeetingStatus.PLANNED);

        Meeting saved = meetingRepository.save(meeting);
        return toFullResponse(saved, lead);
    }

    /**
     * Update core meeting fields (scheduled date, status, energy).
     *
     * @param lead      authenticated user
     * @param meetingId meeting to update
     * @param request   updated fields with version
     * @return updated meeting
     */
    @Transactional
    public MeetingResponse updateMeeting(UserDetailsImpl lead, UUID meetingId, UpdateMeetingRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (!meeting.getLead().getId().equals(lead.getId())) {
            throw new AccessDeniedException("Only the meeting author may update it");
        }

        if (meeting.getVersion() == null || !meeting.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Meeting.class, meetingId);
        }

        meeting.setScheduledDate(request.scheduledDate());
        meeting.setStatus(request.status());
        meeting.setEnergyScore(request.energyScore());
        meeting.setEnergyNote(request.energyNote());

        Meeting updated = meetingRepository.save(meeting);
        return toFullResponse(updated, lead);
    }

    /**
     * Delete a meeting and all its actions.
     *
     * @param lead      authenticated user
     * @param meetingId meeting to delete
     */
    @Transactional
    public void deleteMeeting(UserDetailsImpl lead, UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (!meeting.getLead().getId().equals(lead.getId())) {
            throw new AccessDeniedException("Only the meeting author may delete it");
        }

        actionRepository.findAllByMeetingId(meetingId)
                .forEach(actionRepository::delete);

        meetingRepository.delete(meeting);
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    /**
     * Get a single meeting by ID with visibility-adjusted notes.
     *
     * @param user      requesting user
     * @param meetingId meeting to fetch
     * @return meeting response
     */
    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(UserDetailsImpl user, UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (!canAccessMeeting(user, meeting)) {
            throw new AccessDeniedException("You do not have access to this meeting");
        }

        return toFullResponse(meeting, user);
    }

    /**
     * List meetings visible to the user.
     *
     * <p>For TEAM_MEMBER: returns meetings where they are the participant.
     * For leads: returns meetings they authored, or all team meetings
     * (depending on caller preference).</p>
     *
     * @param user     requesting user
     * @param asLead   if true, return authored meetings; if false and user
     *                 is a member, return meetings where they participated
     * @param pageable pagination
     * @return page of meetings
     */
    @Transactional(readOnly = true)
    public Page<MeetingResponse> listMeetings(UserDetailsImpl user, boolean asLead, Pageable pageable) {
        if (asLead) {
            return toFullResponsePage(meetingRepository.findAllByLeadIdOrderByScheduledDateDesc(user.getId(), pageable), user);
        }
        return toFullResponsePage(meetingRepository.findAllByParticipantIdOrderByScheduledDateDesc(user.getId(), pageable), user);
    }

    /**
     * List all meetings in a team (for lead dashboard use).
     */
    @Transactional(readOnly = true)
    public Page<MeetingResponse> listTeamMeetings(UserDetailsImpl user, UUID teamId, Pageable pageable) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
        return toFullResponsePage(meetingRepository.findAllByTeamIdOrderByScheduledDateDesc(teamId, pageable), user);
    }

    // ------------------------------------------------------------------
    // Notes
    // ------------------------------------------------------------------

    /**
     * Update shared or private notes on a meeting.
     *
     * @param user      requesting user
     * @param meetingId target meeting
     * @param request   notes payload with optimistic-lock version
     * @return updated meeting
     */
    @Transactional
    public MeetingResponse updateNotes(UserDetailsImpl user, UUID meetingId, UpdateNotesRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (meeting.getVersion() == null || !meeting.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Meeting.class, meetingId);
        }

        // sharedNotes: any lead of the team may edit
        if (request.sharedNotes() != null) {
            if (!leadsScope(user.getId(), meeting)) {
                throw new AccessDeniedException("Only team leads may edit shared notes");
            }
            meeting.setSharedNotes(request.sharedNotes());
        }

        // privateNotes: only the author
        if (request.privateNotes() != null) {
            if (!meeting.getLead().getId().equals(user.getId())) {
                throw new AccessDeniedException("Only the meeting author may edit private notes");
            }
            meeting.setPrivateNotes(request.privateNotes());
        }

        Meeting updated = meetingRepository.save(meeting);
        return toFullResponse(updated, user);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /**
     * Create an action item on a meeting.
     *
     * @param user      requesting user (must be lead of the meeting's team)
     * @param meetingId originating meeting
     * @param request   action data
     * @return created action
     */
    @Transactional
    public ActionResponse createAction(UserDetailsImpl user, UUID meetingId, ActionRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (!leadsScope(user.getId(), meeting)) {
            throw new AccessDeniedException("Only team leads may add actions");
        }

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Action owner not found"));

        MeetingAction action = new MeetingAction();
        action.setMeeting(meeting);
        action.setText(request.text().trim());
        action.setOwner(owner);
        action.setDueDate(request.dueDate());
        action.setStatus(request.status() != null ? request.status() : ActionStatus.OPEN);

        MeetingAction saved = actionRepository.save(action);
        return toActionResponse(saved);
    }

    /**
     * Update an action item (status, text, due date).
     *
     * @throws NotFoundException if the action does not belong to {@code meetingId}
     */
    @Transactional
    public ActionResponse updateAction(UserDetailsImpl user, UUID meetingId, UUID actionId, ActionRequest request) {
        MeetingAction action = findActionInMeeting(meetingId, actionId);

        // Owner or team lead may update
        if (!action.getOwner().getId().equals(user.getId())
                && !leadsScope(user.getId(), action.getMeeting())) {
            throw new AccessDeniedException("Not allowed to update this action");
        }

        action.setText(request.text().trim());
        action.setDueDate(request.dueDate());
        if (request.status() != null) {
            action.setStatus(request.status());
        }

        MeetingAction updated = actionRepository.save(action);
        return toActionResponse(updated);
    }

    /**
     * Delete an action item.
     *
     * @throws NotFoundException if the action does not belong to {@code meetingId}
     */
    @Transactional
    public void deleteAction(UserDetailsImpl user, UUID meetingId, UUID actionId) {
        MeetingAction action = findActionInMeeting(meetingId, actionId);

        if (!leadsScope(user.getId(), action.getMeeting())) {
            throw new AccessDeniedException("Only team leads may delete actions");
        }
        actionRepository.delete(action);
    }

    /**
     * List actions for a meeting.
     */
    @Transactional(readOnly = true)
    public List<ActionResponse> listActions(UserDetailsImpl user, UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        if (!canAccessMeeting(user, meeting)) {
            throw new AccessDeniedException("No access to this meeting");
        }

        return actionRepository.findAllByMeetingId(meetingId)
                .stream()
                .map(this::toActionResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Loads an action and checks that it belongs to the meeting from the URL.
     * Without this check a lead of one team could address actions of another meeting by id.
     */
    private MeetingAction findActionInMeeting(UUID meetingId, UUID actionId) {
        MeetingAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action not found"));
        if (!action.getMeeting().getId().equals(meetingId)) {
            throw new NotFoundException("Action not found");
        }
        return action;
    }

    private boolean canAccessMeeting(UserDetailsImpl user, Meeting meeting) {
        if (meeting.getParticipant().getId().equals(user.getId())) return true;
        if (meeting.getLead().getId().equals(user.getId())) return true;
        return leadsScope(user.getId(), meeting);
    }

    private boolean isTeamLead(UUID userId, UUID teamId) {
        return teamMembershipRepository
                .findByUserIdAndTeamIdAndRole(userId, teamId, UserRole.TEAM_LEAD)
                .isPresent()
                || teamMembershipRepository
                .findByUserIdAndTeamIdAndRole(userId, teamId, UserRole.IT_LEAD)
                .isPresent();
    }

    private boolean isTeamMember(UUID userId, UUID teamId) {
        return teamMembershipRepository.existsByUserIdAndTeamId(userId, teamId);
    }

    /** Where a new meeting sits in the chain, derived from who leads whom. */
    private record Scope(MeetingLevel level, Team team, Cluster cluster) {}

    /**
     * Finds the level of a 1:1 between a lead and a participant. Each level is one step down:
     * Stream Lead to Cluster Lead, Cluster Lead to Team Lead, Team Lead to Team Member.
     *
     * @throws AccessDeniedException if the lead has no 1:1 relation with the participant
     */
    private Scope resolveScope(UUID leadId, UUID participantId) {
        var participantMemberships = teamMembershipRepository.findAllByUserId(participantId, Pageable.unpaged()).getContent();

        // TEAM: the lead leads the participant's team, and the participant is a team member
        for (TeamMembership pm : participantMemberships) {
            if (pm.getRole() == UserRole.TEAM_MEMBER && isTeamLead(leadId, pm.getTeam().getId())) {
                return new Scope(MeetingLevel.TEAM, pm.getTeam(), pm.getTeam().getCluster());
            }
        }

        // CLUSTER: the lead leads the cluster, and the participant leads a team in it
        Set<UUID> ledClusters = leadershipResolver.ledClusterIds(leadId);
        for (TeamMembership pm : participantMemberships) {
            boolean teamLeadRole = pm.getRole() == UserRole.TEAM_LEAD || pm.getRole() == UserRole.IT_LEAD;
            Cluster cluster = pm.getTeam().getCluster();
            if (teamLeadRole && ledClusters.contains(cluster.getId())) {
                return new Scope(MeetingLevel.CLUSTER, pm.getTeam(), cluster);
            }
        }

        // STREAM: the lead leads the stream, and the participant leads a cluster in it
        Set<UUID> ledStreams = leadershipResolver.ledStreamIds(leadId);
        for (Cluster cluster : leadershipResolver.clustersLedBy(participantId)) {
            if (ledStreams.contains(cluster.getStream().getId())) {
                return new Scope(MeetingLevel.STREAM, null, cluster);
            }
        }

        throw new AccessDeniedException("No 1:1 relation between the lead and the participant");
    }

    /**
     * True if the user leads the meeting's scope: its team (TEAM), its cluster (CLUSTER),
     * or its stream (STREAM).
     */
    private boolean leadsScope(UUID userId, Meeting meeting) {
        return switch (meeting.getLevel()) {
            case TEAM -> isTeamLead(userId, meeting.getTeam().getId());
            case CLUSTER -> leadershipResolver.ledClusterIds(userId).contains(meeting.getCluster().getId());
            case STREAM -> leadershipResolver.ledStreamIds(userId).contains(meeting.getCluster().getStream().getId());
        };
    }

    /** Same as {@link #leadsScope} but against precomputed sets, for lists. */
    private boolean isLeadInScope(Meeting meeting, Set<UUID> teamIds, Set<UUID> clusterIds, Set<UUID> streamIds) {
        return switch (meeting.getLevel()) {
            case TEAM -> teamIds.contains(meeting.getTeam().getId());
            case CLUSTER -> clusterIds.contains(meeting.getCluster().getId());
            case STREAM -> streamIds.contains(meeting.getCluster().getStream().getId());
        };
    }

    private Page<MeetingResponse> toFullResponsePage(Page<Meeting> page, UserDetailsImpl viewer) {
        return new PageImpl<>(toFullResponses(page.getContent(), viewer), page.getPageable(), page.getTotalElements());
    }

    private MeetingResponse toFullResponse(Meeting m, UserDetailsImpl viewer) {
        return toFullResponses(List.of(m), viewer).get(0);
    }

    /**
     * Builds responses for several meetings with a fixed number of queries: one for the
     * viewer's lead memberships and one for all actions of the meetings. Doing it per meeting
     * costs one query per meeting.
     */
    private List<MeetingResponse> toFullResponses(List<Meeting> meetings, UserDetailsImpl viewer) {
        if (meetings.isEmpty()) {
            return List.of();
        }

        Set<UUID> leadTeamIds = teamMembershipRepository.findAllByUserIdAndRoleIn(viewer.getId(), LEAD_ROLES)
                .stream()
                .map(tm -> tm.getTeam().getId())
                .collect(Collectors.toSet());

        List<UUID> meetingIds = meetings.stream().map(Meeting::getId).toList();
        Map<UUID, List<ActionResponse>> actionsByMeeting = actionRepository.findAllByMeetingIdIn(meetingIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getMeeting().getId(),
                        Collectors.mapping(this::toActionResponse, Collectors.toList())));

        Set<UUID> leadClusterIds = leadershipResolver.ledClusterIds(viewer.getId());
        Set<UUID> leadStreamIds = leadershipResolver.ledStreamIds(viewer.getId());

        return meetings.stream()
                .map(m -> toMeetingResponse(m, viewer,
                        isLeadInScope(m, leadTeamIds, leadClusterIds, leadStreamIds),
                        actionsByMeeting.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    private MeetingResponse toMeetingResponse(Meeting m, UserDetailsImpl viewer, boolean isLead,
                                              List<ActionResponse> actions) {
        boolean isAuthor = m.getLead().getId().equals(viewer.getId());

        String sharedNotes = isLead ? m.getSharedNotes() : null;
        String privateNotes = isAuthor ? m.getPrivateNotes() : null;

        return new MeetingResponse(
                m.getId(),
                m.getParticipant().getId(),
                fullName(m.getParticipant()),
                m.getLead().getId(),
                fullName(m.getLead()),
                m.getTeam() == null ? null : m.getTeam().getId(),
                m.getLevel(),
                m.getCluster().getId(),
                m.getScheduledDate(),
                m.getStatus(),
                m.getEnergyScore(),
                m.getEnergyNote(),
                sharedNotes,
                privateNotes,
                actions,
                m.getVersion(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    private ActionResponse toActionResponse(MeetingAction a) {
        return new ActionResponse(
                a.getId(),
                a.getMeeting().getId(),
                a.getText(),
                a.getOwner().getId(),
                fullName(a.getOwner()),
                a.getDueDate(),
                a.getStatus(),
                a.getVersion(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
