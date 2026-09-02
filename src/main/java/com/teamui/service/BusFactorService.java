package com.teamui.service;

import com.teamui.domain.enums.Criticality;
import com.teamui.domain.enums.ExpertiseLevel;
import com.teamui.domain.enums.UserRole;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.system.ITSystem;
import com.teamui.domain.system.ITSystemRepository;
import com.teamui.domain.system.SystemExpertise;
import com.teamui.domain.system.SystemExpertiseRepository;
import com.teamui.domain.team.Team;
import com.teamui.domain.team.TeamRepository;
import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for Bus Factor management: system registration, expertise
 * allocation, risk alerts, and the team knowledge matrix.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class BusFactorService {

    private final ITSystemRepository systemRepository;
    private final SystemExpertiseRepository expertiseRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    public BusFactorService(ITSystemRepository systemRepository,
                            SystemExpertiseRepository expertiseRepository,
                            TeamRepository teamRepository,
                            TeamMembershipRepository teamMembershipRepository,
                            UserRepository userRepository) {
        this.systemRepository = systemRepository;
        this.expertiseRepository = expertiseRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // Systems CRUD
    // ------------------------------------------------------------------

    /**
     * Register a new system for Bus Factor tracking.
     */
    @Transactional
    public SystemResponse createSystem(UserDetailsImpl user, CreateSystemRequest request) {
        if (!isTeamLead(user.getId(), request.teamId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Only team leads may register systems");
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        ITSystem sys = new ITSystem();
        sys.setName(request.name().trim());
        sys.setDescription(request.description());
        sys.setCriticality(request.criticality());
        sys.setTeam(team);

        ITSystem saved = systemRepository.save(sys);
        return toSystemResponse(saved);
    }

    /**
     * Update system fields.
     */
    @Transactional
    public SystemResponse updateSystem(UserDetailsImpl user, UUID systemId, UpdateSystemRequest request) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamLead(user.getId(), sys.getTeam().getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Only team leads may update systems");
        }

        sys.setName(request.name().trim());
        sys.setDescription(request.description());
        sys.setCriticality(request.criticality());

        return toSystemResponse(systemRepository.save(sys));
    }

    /**
     * Delete a system and all its expertise links.
     */
    @Transactional
    public void deleteSystem(UserDetailsImpl user, UUID systemId) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamLead(user.getId(), sys.getTeam().getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Only team leads may delete systems");
        }

        expertiseRepository.findAllBySystemId(systemId, Pageable.unpaged())
                .forEach(expertiseRepository::delete);

        systemRepository.delete(sys);
    }

    /**
     * Get a single system.
     */
    @Transactional(readOnly = true)
    public SystemResponse getSystem(UserDetailsImpl user, UUID systemId) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamMember(user.getId(), sys.getTeam().getId())) {
            throw new AccessDeniedException("Not a member of this team");
        }

        return toSystemResponse(sys);
    }

    /**
     * List systems for a team.
     */
    @Transactional(readOnly = true)
    public List<SystemResponse> listSystemsByTeam(UserDetailsImpl user, UUID teamId) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
        return systemRepository.findAllByTeamId(teamId).stream()
                .map(this::toSystemResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Expertise
    // ------------------------------------------------------------------

    /**
     * Assign or update a user's expertise on a system.
     */
    @Transactional
    public ExpertiseResponse assignExpertise(UserDetailsImpl actor, UUID systemId, ExpertiseRequest request) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamLead(actor.getId(), sys.getTeam().getId()) && !isAdmin(actor)) {
            throw new AccessDeniedException("Only team leads may assign expertise");
        }

        User employee = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SystemExpertise exp = expertiseRepository.findBySystemIdAndUserId(systemId, request.userId())
                .orElseGet(() -> {
                    SystemExpertise e = new SystemExpertise();
                    e.setSystem(sys);
                    e.setUser(employee);
                    return e;
                });

        exp.setLevel(request.level());
        SystemExpertise saved = expertiseRepository.save(exp);
        return toExpertiseResponse(saved);
    }

    /**
     * Remove a user's expertise link from a system.
     */
    @Transactional
    public void removeExpertise(UserDetailsImpl actor, UUID systemId, UUID userId) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamLead(actor.getId(), sys.getTeam().getId()) && !isAdmin(actor)) {
            throw new AccessDeniedException("Only team leads may remove expertise");
        }

        expertiseRepository.findBySystemIdAndUserId(systemId, userId)
                .ifPresent(expertiseRepository::delete);
    }

    /**
     * List expertise entries for a system.
     */
    @Transactional(readOnly = true)
    public Page<ExpertiseResponse> listExpertiseBySystem(UserDetailsImpl user,
                                                          UUID systemId,
                                                          Pageable pageable) {
        ITSystem sys = systemRepository.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("System not found"));

        if (!isTeamMember(user.getId(), sys.getTeam().getId())) {
            throw new AccessDeniedException("Not a member of this team");
        }

        return expertiseRepository.findAllBySystemId(systemId, pageable)
                .map(this::toExpertiseResponse);
    }

    /**
     * List expertise entries for a user.
     */
    @Transactional(readOnly = true)
    public Page<ExpertiseResponse> listExpertiseByUser(UserDetailsImpl viewer,
                                                        UUID userId,
                                                        Pageable pageable) {
        // Users can see their own; leads can see team members'
        if (!viewer.getId().equals(userId) && !isTeamLeadForUser(viewer.getId(), userId) && !isAdmin(viewer)) {
            throw new AccessDeniedException("Not allowed to view this user's expertise");
        }

        return expertiseRepository.findAllByUserId(userId, pageable)
                .map(this::toExpertiseResponse);
    }

    // ------------------------------------------------------------------
    // Bus Factor Alerts
    // ------------------------------------------------------------------

    /**
     * Return systems in a team that are at risk (low expert coverage).
     */
    @Transactional(readOnly = true)
    public List<BusFactorAlertDto> getAlertsForTeam(UserDetailsImpl user, UUID teamId) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        return systemRepository.findAllByTeamId(teamId).stream()
                .map(sys -> buildAlert(sys, teamId))
                .filter(BusFactorAlertDto::atRisk)
                .toList();
    }

    /**
     * Return all systems with coverage stats (not just at-risk).
     */
    @Transactional(readOnly = true)
    public List<BusFactorAlertDto> getFullMatrixForTeam(UserDetailsImpl user, UUID teamId) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        return systemRepository.findAllByTeamId(teamId).stream()
                .map(sys -> buildAlert(sys, teamId))
                .toList();
    }

    // ------------------------------------------------------------------
    // Knowledge Matrix
    // ------------------------------------------------------------------

    /**
     * Return the team knowledge matrix: systems with people grouped by level.
     */
    @Transactional(readOnly = true)
    public List<SystemWithPeopleDto> getKnowledgeMatrix(UserDetailsImpl user, UUID teamId) {
        if (!isTeamMember(user.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        return systemRepository.findAllByTeamId(teamId).stream()
                .map(sys -> {
                    Page<SystemExpertise> entries = expertiseRepository
                            .findAllBySystemId(sys.getId(), Pageable.unpaged());

                    List<String> experts = entries.stream()
                            .filter(e -> e.getLevel() == ExpertiseLevel.EXPERT)
                            .map(e -> fullName(e.getUser()))
                            .toList();

                    List<String> advanced = entries.stream()
                            .filter(e -> e.getLevel() == ExpertiseLevel.ADVANCED)
                            .map(e -> fullName(e.getUser()))
                            .toList();

                    List<String> basic = entries.stream()
                            .filter(e -> e.getLevel() == ExpertiseLevel.BASIC)
                            .map(e -> fullName(e.getUser()))
                            .toList();

                    return new SystemWithPeopleDto(
                            sys.getId(), sys.getName(), sys.getCriticality(),
                            experts, advanced, basic
                    );
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private BusFactorAlertDto buildAlert(ITSystem sys, UUID teamId) {
        Page<SystemExpertise> entries = expertiseRepository
                .findAllBySystemId(sys.getId(), Pageable.unpaged());

        long expertCount = entries.stream().filter(e -> e.getLevel() == ExpertiseLevel.EXPERT).count();
        long advancedCount = entries.stream().filter(e -> e.getLevel() == ExpertiseLevel.ADVANCED).count();
        int totalMembers = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged()).getContent().size();

        List<String> expertNames = entries.stream()
                .filter(e -> e.getLevel() == ExpertiseLevel.EXPERT)
                .map(e -> fullName(e.getUser()))
                .toList();

        boolean atRisk = expertCount == 0 || (expertCount == 1 && sys.getCriticality().ordinal() <= Criticality.HIGH.ordinal());

        return new BusFactorAlertDto(
                sys.getId(), sys.getName(), sys.getCriticality(),
                expertCount, advancedCount, totalMembers, atRisk, expertNames
        );
    }

    private SystemResponse toSystemResponse(ITSystem sys) {
        return new SystemResponse(
                sys.getId(),
                sys.getName(),
                sys.getDescription(),
                sys.getCriticality(),
                sys.getTeam().getId(),
                sys.getTeam().getName(),
                sys.getCreatedAt()
        );
    }

    private ExpertiseResponse toExpertiseResponse(SystemExpertise e) {
        return new ExpertiseResponse(
                e.getId(),
                e.getSystem().getId(),
                e.getSystem().getName(),
                e.getUser().getId(),
                fullName(e.getUser()),
                e.getLevel(),
                e.getCreatedAt()
        );
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

    private boolean isTeamLeadForUser(UUID leadId, UUID userId) {
        var memberships = teamMembershipRepository.findAllByUserId(userId, Pageable.unpaged()).getContent();
        for (var m : memberships) {
            UUID teamId = m.getTeam().getId();
            if (isTeamLead(leadId, teamId)) return true;
        }
        return false;
    }

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
