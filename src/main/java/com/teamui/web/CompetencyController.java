package com.teamui.web;

import com.teamui.domain.competency.Competency;
import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.CompetencyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for the star-radar competency assessment model.
 *
 * <p>Endpoints grouped by:</p>
 * <ul>
 *   <li>Competency axes — {@code /api/competencies}</li>
 *   <li>Scores — {@code /api/competencies/scores}</li>
 *   <li>Radar view — {@code /api/competencies/radar/{userId}}</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/competencies")
public class CompetencyController {

    private final CompetencyService competencyService;

    public CompetencyController(CompetencyService competencyService) {
        this.competencyService = competencyService;
    }

    // ------------------------------------------------------------------
    // Axes
    // ------------------------------------------------------------------

    /**
     * Return all competency axes in display order.
     */
    @GetMapping
    public ResponseEntity<List<Competency>> listAxes() {
        return ResponseEntity.ok(competencyService.listCompetencies());
    }

    // ------------------------------------------------------------------
    // Scores
    // ------------------------------------------------------------------

    /**
     * List competency scores for a user (both self and lead ratings).
     */
    @GetMapping("/scores/user/{userId}")
    public ResponseEntity<Page<CompetencyScoreResponse>> getScoresForUser(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(competencyService.getScoresForUser(user, userId, pageable));
    }

    /**
     * Submit or update a self-assessment.
     */
    @PostMapping("/scores/user/{userId}/self")
    public ResponseEntity<CompetencyScoreResponse> submitSelfScore(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            @RequestBody @Valid ScoreSelfRequest request) {
        CompetencyScoreResponse resp = competencyService.submitSelfScore(user, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Submit or update a lead assessment.
     */
    @PostMapping("/scores/user/{userId}/lead")
    public ResponseEntity<CompetencyScoreResponse> submitLeadScore(
            @AuthenticationPrincipal UserDetailsImpl lead,
            @PathVariable UUID userId,
            @RequestBody @Valid ScoreLeadRequest request) {
        CompetencyScoreResponse resp = competencyService.submitLeadScore(lead, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // ------------------------------------------------------------------
    // Radar
    // ------------------------------------------------------------------

    /**
     * Build a star-radar view for an employee.
     */
    @GetMapping("/radar/{userId}")
    public ResponseEntity<RadarResponse> getRadar(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(competencyService.getRadar(user, userId));
    }

    /**
     * Build a team-average star-radar.
     */
    @GetMapping("/radar/team/{teamId}")
    public ResponseEntity<TeamRadarResponse> getTeamRadar(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(competencyService.getTeamRadar(user, teamId));
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    /**
     * List historical competency snapshots for a user.
     *
     * @param competencyId optional filter by competency
     */
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<Page<CompetencyScoreHistoryResponse>> getHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID competencyId,
            Pageable pageable) {
        return ResponseEntity.ok(competencyService.getScoreHistory(user, userId, competencyId, pageable));
    }

    // ------------------------------------------------------------------
    // Trend line
    // ------------------------------------------------------------------

    /**
     * Build a trend line for a user's competency.
     *
     * @param granularity "week" (default) or "month"
     */
    @GetMapping("/trend/user/{userId}/competency/{competencyId}")
    public ResponseEntity<TrendResponse> getTrend(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            @PathVariable UUID competencyId,
            @RequestParam(defaultValue = "week") String granularity) {
        return ResponseEntity.ok(competencyService.getTrend(user, userId, competencyId, granularity));
    }

    // ------------------------------------------------------------------
    // Benchmark
    // ------------------------------------------------------------------

    /**
     * Overlay an employee radar on top of the team average.
     */
    @GetMapping("/benchmark/user/{userId}/team/{teamId}")
    public ResponseEntity<BenchmarkResponse> getBenchmark(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(competencyService.getBenchmark(user, userId, teamId));
    }
}
