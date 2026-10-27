package com.teamui.web;

import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.MeetingParticipantService;
import com.teamui.service.MeetingService;
import jakarta.validation.Valid;
import com.teamui.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for 1:1 meetings and their action items.
 *
 * <p>Endpoints grouped by:</p>
 * <ul>
 *   <li>Meeting CRUD — {@code /api/meetings}</li>
 *   <li>Notes — {@code /api/meetings/{id}/notes}</li>
 *   <li>Actions — {@code /api/meetings/{id}/actions}</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingParticipantService participantService;

    public MeetingController(MeetingService meetingService, MeetingParticipantService participantService) {
        this.meetingService = meetingService;
        this.participantService = participantService;
    }

    // ------------------------------------------------------------------
    // Meeting CRUD
    // ------------------------------------------------------------------

    /**
     * People the caller can schedule a 1:1 with, each with the level of the meeting.
     */
    @GetMapping("/participants")
    public ResponseEntity<List<MeetingParticipantDto>> participants(@AuthenticationPrincipal UserDetailsImpl lead) {
        return ResponseEntity.ok(participantService.listParticipants(lead.getId()));
    }

    /**
     * Schedule a new 1:1 meeting.
     */
    @PostMapping
    public ResponseEntity<MeetingResponse> create(
            @AuthenticationPrincipal UserDetailsImpl lead,
            @RequestBody @Valid CreateMeetingRequest request) {
        MeetingResponse response = meetingService.createMeeting(lead, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a meeting by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> get(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(meetingService.getMeeting(user, id));
    }

    /**
     * Update core meeting fields.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MeetingResponse> update(
            @AuthenticationPrincipal UserDetailsImpl lead,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateMeetingRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(lead, id, request));
    }

    /**
     * Delete a meeting.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetailsImpl lead,
            @PathVariable UUID id) {
        meetingService.deleteMeeting(lead, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * List meetings visible to the caller.
     *
     * @param asLead if true, return meetings authored by the caller;
     *               otherwise return meetings where caller is participant
     */
    @GetMapping
    public ResponseEntity<Page<MeetingResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "false") boolean asLead,
            Pageable pageable) {
        return ResponseEntity.ok(meetingService.listMeetings(user, asLead, pageable));
    }

    /**
     * List all meetings for a team.
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<Page<MeetingResponse>> listByTeam(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId,
            Pageable pageable) {
        return ResponseEntity.ok(meetingService.listTeamMeetings(user, teamId, pageable));
    }

    // ------------------------------------------------------------------
    // Notes
    // ------------------------------------------------------------------

    /**
     * Update shared or private notes on a meeting.
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<MeetingResponse> updateNotes(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateNotesRequest request) {
        return ResponseEntity.ok(meetingService.updateNotes(user, id, request));
    }

    /**
     * Update the participant's agenda for the meeting.
     */
    @PutMapping("/{id}/agenda")
    public ResponseEntity<MeetingResponse> updateAgenda(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateAgendaRequest request) {
        return ResponseEntity.ok(meetingService.updateAgenda(user, id, request));
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /**
     * Create an action item on a meeting.
     */
    @PostMapping("/{meetingId}/actions")
    public ResponseEntity<ActionResponse> createAction(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID meetingId,
            @RequestBody @Valid ActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.createAction(user, meetingId, request));
    }

    /**
     * List actions for a meeting.
     */
    @GetMapping("/{meetingId}/actions")
    public ResponseEntity<List<ActionResponse>> listActions(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID meetingId) {
        return ResponseEntity.ok(meetingService.listActions(user, meetingId));
    }

    /**
     * Update an action item.
     */
    @PutMapping("/{meetingId}/actions/{actionId}")
    public ResponseEntity<ActionResponse> updateAction(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID meetingId,
            @PathVariable UUID actionId,
            @RequestBody @Valid ActionRequest request) {
        return ResponseEntity.ok(meetingService.updateAction(user, meetingId, actionId, request));
    }

    /**
     * Delete an action item.
     */
    @DeleteMapping("/{meetingId}/actions/{actionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAction(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID meetingId,
            @PathVariable UUID actionId) {
        meetingService.deleteAction(user, meetingId, actionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
