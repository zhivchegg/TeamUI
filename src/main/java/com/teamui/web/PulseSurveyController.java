package com.teamui.web;

import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.PulseSurveyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for pulse surveys: pre-1:1 check-ins, team health aggregates,
 * and energy trend lines.
 *
 * <p>Endpoints grouped by:</p>
 * <ul>
 *   <li>Personal — {@code /api/pulse}</li>
 *   <li>Team aggregates — {@code /api/pulse/team/{teamId}}</li>
 *   <li>Trends — {@code /api/pulse/team/{teamId}/trend}</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/pulse")
public class PulseSurveyController {

    private final PulseSurveyService pulseSurveyService;

    public PulseSurveyController(PulseSurveyService pulseSurveyService) {
        this.pulseSurveyService = pulseSurveyService;
    }

    // ------------------------------------------------------------------
    // Personal
    // ------------------------------------------------------------------

    /**
     * Submit a new pulse check-in.
     */
    @PostMapping
    public ResponseEntity<PulseSurveyResponse> submit(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody @Valid SubmitPulseRequest request) {
        PulseSurveyResponse resp = pulseSurveyService.submit(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Get a single pulse survey.
     */
    @GetMapping("/{surveyId}")
    public ResponseEntity<PulseSurveyResponse> get(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID surveyId) {
        return ResponseEntity.ok(pulseSurveyService.getSurvey(user, surveyId));
    }

    /**
     * Delete a pulse survey.
     */
    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID surveyId) {
        pulseSurveyService.deleteSurvey(user, surveyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List pulse surveys for a user (own or team member for leads).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PulseSurveyResponse>> listByUser(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(pulseSurveyService.listByUser(user, userId, pageable));
    }

    // ------------------------------------------------------------------
    // Team aggregates
    // ------------------------------------------------------------------

    /**
     * Aggregated pulse stats for a team.
     *
     * @param daysBack number of days to include (default 30)
     */
    @GetMapping("/team/{teamId}/aggregate")
    public ResponseEntity<TeamPulseAggregate> getTeamAggregate(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "30") int daysBack) {
        return ResponseEntity.ok(pulseSurveyService.getTeamAggregate(user, teamId, daysBack));
    }

    /**
     * Energy trend line for a team.
     *
     * @param granularity "week" (default) or "month"
     * @param daysBack    number of days to include (default 90)
     */
    @GetMapping("/team/{teamId}/trend")
    public ResponseEntity<PulseTrendResponse> getTeamTrend(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "week") String granularity,
            @RequestParam(defaultValue = "90") int daysBack) {
        return ResponseEntity.ok(pulseSurveyService.getTeamTrend(user, teamId, granularity, daysBack));
    }
}
