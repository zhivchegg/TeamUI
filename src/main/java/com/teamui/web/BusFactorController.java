package com.teamui.web;

import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.BusFactorService;
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
 * REST API for Bus Factor management: system registration, expertise allocation,
 * risk alerts, and the team knowledge matrix.
 *
 * <p>Endpoints grouped by:</p>
 * <ul>
 *   <li>Systems — {@code /api/systems}</li>
 *   <li>Expertise — {@code /api/systems/{systemId}/expertise}</li>
 *   <li>Alerts / Matrix — {@code /api/systems/team/{teamId}}</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/systems")
public class BusFactorController {

    private final BusFactorService busFactorService;

    public BusFactorController(BusFactorService busFactorService) {
        this.busFactorService = busFactorService;
    }

    // ------------------------------------------------------------------
    // Systems CRUD
    // ------------------------------------------------------------------

    /**
     * Register a new system for Bus Factor tracking.
     */
    @PostMapping
    public ResponseEntity<SystemResponse> create(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody @Valid CreateSystemRequest request) {
        SystemResponse resp = busFactorService.createSystem(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Get a system by ID.
     */
    @GetMapping("/{systemId}")
    public ResponseEntity<SystemResponse> get(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId) {
        return ResponseEntity.ok(busFactorService.getSystem(user, systemId));
    }

    /**
     * Update a system.
     */
    @PutMapping("/{systemId}")
    public ResponseEntity<SystemResponse> update(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId,
            @RequestBody @Valid UpdateSystemRequest request) {
        return ResponseEntity.ok(busFactorService.updateSystem(user, systemId, request));
    }

    /**
     * Delete a system and all its expertise links.
     */
    @DeleteMapping("/{systemId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId) {
        busFactorService.deleteSystem(user, systemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List systems for a team.
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<SystemResponse>> listByTeam(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(busFactorService.listSystemsByTeam(user, teamId));
    }

    // ------------------------------------------------------------------
    // Expertise
    // ------------------------------------------------------------------

    /**
     * Assign or update expertise for a user on a system.
     */
    @PostMapping("/{systemId}/expertise")
    public ResponseEntity<ExpertiseResponse> assignExpertise(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId,
            @RequestBody @Valid ExpertiseRequest request) {
        ExpertiseResponse resp = busFactorService.assignExpertise(user, systemId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Remove a user's expertise from a system.
     */
    @DeleteMapping("/{systemId}/expertise/{userId}")
    public ResponseEntity<Void> removeExpertise(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId,
            @PathVariable UUID userId) {
        busFactorService.removeExpertise(user, systemId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List expertise entries for a system.
     */
    @GetMapping("/{systemId}/expertise")
    public ResponseEntity<Page<ExpertiseResponse>> listExpertiseBySystem(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID systemId,
            Pageable pageable) {
        return ResponseEntity.ok(busFactorService.listExpertiseBySystem(user, systemId, pageable));
    }

    /**
     * List expertise entries for a user.
     */
    @GetMapping("/expertise/user/{userId}")
    public ResponseEntity<Page<ExpertiseResponse>> listExpertiseByUser(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(busFactorService.listExpertiseByUser(user, userId, pageable));
    }

    // ------------------------------------------------------------------
    // Alerts & Matrix
    // ------------------------------------------------------------------

    /**
     * Return at-risk systems for a team (bus factor &lt;= 1).
     */
    @GetMapping("/alerts/team/{teamId}")
    public ResponseEntity<List<BusFactorAlertDto>> getAlerts(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(busFactorService.getAlertsForTeam(user, teamId));
    }

    /**
     * Return full coverage matrix for a team (all systems with stats).
     */
    @GetMapping("/matrix/team/{teamId}")
    public ResponseEntity<List<BusFactorAlertDto>> getFullMatrix(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(busFactorService.getFullMatrixForTeam(user, teamId));
    }

    /**
     * Return the knowledge matrix: systems with people grouped by level.
     */
    @GetMapping("/knowledge/team/{teamId}")
    public ResponseEntity<List<SystemWithPeopleDto>> getKnowledgeMatrix(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(busFactorService.getKnowledgeMatrix(user, teamId));
    }
}
