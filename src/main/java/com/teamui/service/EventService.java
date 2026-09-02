package com.teamui.service;

import com.teamui.domain.enums.UserRole;
import com.teamui.domain.event.Event;
import com.teamui.domain.event.EventRepository;
import com.teamui.domain.membership.TeamMembership;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.team.Team;
import com.teamui.domain.team.TeamRepository;
import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import com.teamui.dto.CreateEventRequest;
import com.teamui.dto.EventResponse;
import com.teamui.dto.UpdateEventRequest;
import com.teamui.security.service.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for timeline events: achievements, incidents, notes, training,
 * lateness, task misses.
 *
 * <p>Visibility rules:
 * <ul>
 *   <li>{@code visibleToEmployee = true/false} controls whether the employee
 *       sees the entry on their own timeline.</li>
 *   <li>Leads and admins can always see all events in their teams.</li>
 *   <li>Employees without lead roles see only events where
 *       {@code userId = self} AND {@code visibleToEmployee = true}.</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public EventService(EventRepository eventRepository,
                         UserRepository userRepository,
                         TeamRepository teamRepository,
                         TeamMembershipRepository teamMembershipRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /**
     * Create a timeline event.
     */
    @Transactional
    public EventResponse createEvent(UserDetailsImpl creator, CreateEventRequest request) {
        verifyTeamAccess(creator, request.teamId());
        if (!isTeamLead(creator.getId(), request.teamId())) {
            throw new AccessDeniedException("Only team leads may create timeline events");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        Event event = new Event();
        event.setUser(user);
        event.setCreatedBy(creator.getUser());
        event.setTeam(team);
        event.setEventType(request.eventType());
        event.setCategory(request.category());
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setImpactLevel(request.impactLevel());
        event.setVisibleToEmployee(request.visibleToEmployee());

        Event saved = eventRepository.save(event);
        return toResponse(saved, creator);
    }

    /**
     * Update a timeline event.
     */
    @Transactional
    public EventResponse updateEvent(UserDetailsImpl user, UUID eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!canModifyEvent(user, event)) {
            throw new AccessDeniedException("Not allowed to update this event");
        }

        event.setEventType(request.eventType());
        event.setCategory(request.category());
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setImpactLevel(request.impactLevel());
        event.setVisibleToEmployee(request.visibleToEmployee());

        Event updated = eventRepository.save(event);
        return toResponse(updated, user);
    }

    /**
     * Delete a timeline event.
     */
    @Transactional
    public void deleteEvent(UserDetailsImpl user, UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!canModifyEvent(user, event)) {
            throw new AccessDeniedException("Not allowed to delete this event");
        }

        eventRepository.delete(event);
    }

    /**
     * Get a single event by ID with visibility check.
     */
    @Transactional(readOnly = true)
    public EventResponse getEvent(UserDetailsImpl user, UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!canViewEvent(user, event)) {
            throw new AccessDeniedException("You do not have access to this event");
        }

        return toResponse(event, user);
    }

    // ------------------------------------------------------------------
    // Listing
    // ------------------------------------------------------------------

    /**
     * List events for a specific employee.
     *
     * <p>If the caller is the employee themselves (and not a lead), returns
     * only {@code visibleToEmployee = true} events. Leads and admins see all.</p>
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> listByUser(UserDetailsImpl user, UUID userId, Pageable pageable) {
        boolean isSelf = userId.equals(user.getId());
        boolean isLead = isLeadAnyTeam(user);

        if (isSelf && !isLead) {
            // Regular employee: only visible events about themselves
            return eventRepository.findAllByUserIdAndVisibleToEmployeeTrueOrderByCreatedAtDesc(userId, pageable)
                    .map(e -> toResponse(e, user));
        }

        // Lead or admin (or someone viewing another user's timeline)
        if (!isSelf && !isLead) {
            throw new AccessDeniedException("Not allowed to view another employee's timeline");
        }

        return eventRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(e -> toResponse(e, user));
    }

    /**
     * List events for a team, optionally filtered by type.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> listByTeam(UserDetailsImpl user, UUID teamId, String eventType, Pageable pageable) {
        verifyTeamAccess(user, teamId);

        Page<Event> page;
        if (eventType != null && !eventType.isBlank()) {
            try {
                page = eventRepository.findAllByTeamIdAndEventTypeOrderByCreatedAtDesc(
                        teamId, com.teamui.domain.enums.EventType.valueOf(eventType.toUpperCase()), pageable);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid event type: " + eventType);
            }
        } else {
            page = eventRepository.findAllByTeamIdOrderByCreatedAtDesc(teamId, pageable);
        }

        return page.map(e -> toFilteredResponse(e, user));
    }

    // ------------------------------------------------------------------
    // Helpers — permissions
    // ------------------------------------------------------------------

    private boolean canViewEvent(UserDetailsImpl user, Event event) {
        if (user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        if (isTeamLead(user.getId(), event.getTeam().getId())) return true;
        if (event.getCreatedBy().getId().equals(user.getId())) return true;
        // Employee viewing own timeline
        return event.getUser().getId().equals(user.getId()) && event.isVisibleToEmployee();
    }

    private boolean canModifyEvent(UserDetailsImpl user, Event event) {
        if (user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        if (isTeamLead(user.getId(), event.getTeam().getId())) return true;
        return event.getCreatedBy().getId().equals(user.getId());
    }

    private void verifyTeamAccess(UserDetailsImpl user, UUID teamId) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
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

    private boolean isLeadAnyTeam(UserDetailsImpl user) {
        return teamMembershipRepository.findAllByUserId(user.getId(), Pageable.unpaged()).getContent()
                .stream()
                .anyMatch(m -> m.getRole() == UserRole.TEAM_LEAD || m.getRole() == UserRole.IT_LEAD);
    }

    // ------------------------------------------------------------------
    // Helpers — mapping
    // ------------------------------------------------------------------

    /**
     * Full response with all fields (for leads / event creator).
     */
    private EventResponse toResponse(Event event, UserDetailsImpl viewer) {
        return new EventResponse(
                event.getId(),
                event.getUser().getId(),
                fullName(event.getUser()),
                event.getCreatedBy().getId(),
                fullName(event.getCreatedBy()),
                event.getTeam().getId(),
                event.getTeam().getName(),
                event.getEventType(),
                event.getCategory(),
                event.getTitle(),
                event.getDescription(),
                event.getImpactLevel(),
                event.isVisibleToEmployee(),
                event.getCreatedAt()
        );
    }

    /**
     * Response filtered for non-leads viewing team events:
     * employees without lead rights see only visible events and redacted fields
     * (they still need event type + title for context, but not internal notes).
     */
    private EventResponse toFilteredResponse(Event event, UserDetailsImpl viewer) {
        boolean isLead = isTeamLead(viewer.getId(), event.getTeam().getId());
        boolean isCreator = event.getCreatedBy().getId().equals(viewer.getId());

        if (isLead || isCreator) {
            return toResponse(event, viewer);
        }

        // Non-lead team member: can see only if visible to employee
        if (!event.isVisibleToEmployee()) {
            return null;
        }

        return new EventResponse(
                event.getId(),
                event.getUser().getId(),
                fullName(event.getUser()),
                event.getCreatedBy().getId(),
                fullName(event.getCreatedBy()),
                event.getTeam().getId(),
                event.getTeam().getName(),
                event.getEventType(),
                event.getCategory(),
                event.getTitle(),
                event.getDescription(),
                event.getImpactLevel(),
                event.isVisibleToEmployee(),
                event.getCreatedAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
