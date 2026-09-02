package com.teamui.service;

import com.teamui.domain.enums.ActionStatus;
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
import com.teamui.security.service.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    public MeetingService(MeetingRepository meetingRepository,
                           MeetingActionRepository actionRepository,
                           UserRepository userRepository,
                           TeamMembershipRepository teamMembershipRepository) {
        this.meetingRepository = meetingRepository;
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
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

        Team team = resolveTeamForLeadAndParticipant(lead.getId(), participant.getId());

        // Verify the authenticated user is a lead in this team
        if (!isTeamLead(lead.getId(), team.getId())) {
            throw new AccessDeniedException("Only team leads may create 1:1 meetings");
        }

        Meeting meeting = new Meeting();
        meeting.setParticipant(participant);
        meeting.setLead(lead.getUser());
        meeting.setTeam(team);
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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

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
            return meetingRepository.findAllByLeadIdOrderByScheduledDateDesc(user.getId(), pageable)
                    .map(m -> toFullResponse(m, user));
        }
        return meetingRepository.findAllByParticipantIdOrderByScheduledDateDesc(user.getId(), pageable)
                .map(m -> toFullResponse(m, user));
    }

    /**
     * List all meetings in a team (for lead dashboard use).
     */
    @Transactional(readOnly = true)
    public Page<MeetingResponse> listTeamMeetings(UserDetailsImpl user, UUID teamId, Pageable pageable) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
        return meetingRepository.findAllByTeamIdOrderByScheduledDateDesc(teamId, pageable)
                .map(m -> toFullResponse(m, user));
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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        if (meeting.getVersion() == null || !meeting.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Meeting.class, meetingId);
        }

        // sharedNotes: any lead of the team may edit
        if (request.sharedNotes() != null) {
            if (!isTeamLead(user.getId(), meeting.getTeam().getId())) {
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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        if (!isTeamLead(user.getId(), meeting.getTeam().getId())) {
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
     */
    @Transactional
    public ActionResponse updateAction(UserDetailsImpl user, UUID actionId, ActionRequest request) {
        MeetingAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found"));

        // Owner or team lead may update
        if (!action.getOwner().getId().equals(user.getId())
                && !isTeamLead(user.getId(), action.getMeeting().getTeam().getId())) {
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
     */
    @Transactional
    public void deleteAction(UserDetailsImpl user, UUID actionId) {
        MeetingAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found"));

        if (!isTeamLead(user.getId(), action.getMeeting().getTeam().getId())) {
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
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

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

    private boolean canAccessMeeting(UserDetailsImpl user, Meeting meeting) {
        if (meeting.getParticipant().getId().equals(user.getId())) return true;
        if (meeting.getLead().getId().equals(user.getId())) return true;
        return isTeamLead(user.getId(), meeting.getTeam().getId());
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
        return teamMembershipRepository.findAllByUserId(userId, Pageable.unpaged()).getContent()
                .stream()
                .anyMatch(m -> m.getTeam().getId().equals(teamId));
    }

    private Team resolveTeamForLeadAndParticipant(UUID leadId, UUID participantId) {
        var leadMemberships = teamMembershipRepository.findAllByUserId(leadId, Pageable.unpaged()).getContent();
        var participantMemberships = teamMembershipRepository.findAllByUserId(participantId, Pageable.unpaged()).getContent();

        for (TeamMembership lm : leadMemberships) {
            for (TeamMembership pm : participantMemberships) {
                if (lm.getTeam().getId().equals(pm.getTeam().getId())) {
                    return lm.getTeam();
                }
            }
        }
        throw new IllegalArgumentException("Lead and participant have no common team");
    }

    private MeetingResponse toFullResponse(Meeting m, UserDetailsImpl viewer) {
        boolean isAuthor = m.getLead().getId().equals(viewer.getId());
        boolean isLead = isTeamLead(viewer.getId(), m.getTeam().getId());

        String sharedNotes = isLead ? m.getSharedNotes() : null;
        String privateNotes = isAuthor ? m.getPrivateNotes() : null;

        List<ActionResponse> actions = actionRepository.findAllByMeetingId(m.getId())
                .stream()
                .map(this::toActionResponse)
                .toList();

        return new MeetingResponse(
                m.getId(),
                m.getParticipant().getId(),
                fullName(m.getParticipant()),
                m.getLead().getId(),
                fullName(m.getLead()),
                m.getTeam().getId(),
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
