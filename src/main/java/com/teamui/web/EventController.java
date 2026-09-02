package com.teamui.web;

import com.teamui.dto.CreateEventRequest;
import com.teamui.dto.EventResponse;
import com.teamui.dto.UpdateEventRequest;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for timeline events: achievements, incidents, notes, training,
 * lateness, and task misses.
 *
 * <p>Endpoints grouped by:</p>
 * <ul>
 *   <li>Event CRUD — {@code /api/events}</li>
 *   <li>Team listing — {@code /api/events/team/{teamId}}</li>
 *   <li>User timeline — {@code /api/events/user/{userId}}</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ------------------------------------------------------------------
    // Event CRUD
    // ------------------------------------------------------------------

    /**
     * Create a timeline event (lead only).
     */
    @PostMapping
    public ResponseEntity<EventResponse> create(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody @Valid CreateEventRequest request) {
        EventResponse response = eventService.createEvent(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get an event by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEvent(user, id));
    }

    /**
     * Update a timeline event.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(user, id, request));
    }

    /**
     * Delete a timeline event.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id) {
        eventService.deleteEvent(user, id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Listing
    // ------------------------------------------------------------------

    /**
     * List events for a specific employee's timeline.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<EventResponse>> listByUser(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(eventService.listByUser(user, userId, pageable));
    }

    /**
     * List events for a team, optionally filtered by event type.
     *
     * @param eventType optional filter (e.g., ACHIEVEMENT, INCIDENT, NOTE)
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<Page<EventResponse>> listByTeam(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId,
            @RequestParam(required = false) String eventType,
            Pageable pageable) {
        return ResponseEntity.ok(eventService.listByTeam(user, teamId, eventType, pageable));
    }
}
