package com.teamui.service;

import com.teamui.domain.enums.UserRole;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.pulse.PulseSurvey;
import com.teamui.domain.pulse.PulseSurveyRepository;
import com.teamui.domain.team.Team;
import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import com.teamui.dto.*;
import com.teamui.security.service.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Business logic for pulse surveys: pre-1:1 check-ins, team energy aggregates,
 * burden themes, and trend lines over time.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class PulseSurveyService {

    private final PulseSurveyRepository pulseSurveyRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public PulseSurveyService(PulseSurveyRepository pulseSurveyRepository,
                               UserRepository userRepository,
                               TeamMembershipRepository teamMembershipRepository) {
        this.pulseSurveyRepository = pulseSurveyRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    // ------------------------------------------------------------------
    // Submit / CRUD
    // ------------------------------------------------------------------

    /**
     * Submit a new pulse check-in.
     */
    @Transactional
    public PulseSurveyResponse submit(UserDetailsImpl user, SubmitPulseRequest request) {
        PulseSurvey survey = new PulseSurvey();
        survey.setUser(user.getUser());
        survey.setEnergyScore(request.energyScore());
        survey.setBurdenNote(request.burdenNote());
        survey.setTopics(request.topics());

        PulseSurvey saved = pulseSurveyRepository.save(survey);
        return toResponse(saved);
    }

    /**
     * Get a single pulse survey.
     */
    @Transactional(readOnly = true)
    public PulseSurveyResponse getSurvey(UserDetailsImpl viewer, UUID surveyId) {
        PulseSurvey survey = pulseSurveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        if (!canAccessSurvey(viewer, survey)) {
            throw new AccessDeniedException("Not allowed to view this survey");
        }

        return toResponse(survey);
    }

    /**
     * Delete a pulse survey (own only, or lead/admin).
     */
    @Transactional
    public void deleteSurvey(UserDetailsImpl viewer, UUID surveyId) {
        PulseSurvey survey = pulseSurveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        if (!canModifySurvey(viewer, survey)) {
            throw new AccessDeniedException("Not allowed to delete this survey");
        }

        pulseSurveyRepository.delete(survey);
    }

    // ------------------------------------------------------------------
    // Listing
    // ------------------------------------------------------------------

    /**
     * List pulse surveys for a user.
     */
    @Transactional(readOnly = true)
    public Page<PulseSurveyResponse> listByUser(UserDetailsImpl viewer, UUID userId, Pageable pageable) {
        if (!canAccessUserSurveys(viewer, userId)) {
            throw new AccessDeniedException("Not allowed to view this user's surveys");
        }
        return pulseSurveyRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    // ------------------------------------------------------------------
    // Team aggregates & trends
    // ------------------------------------------------------------------

    /**
     * Aggregated pulse stats for a team over the last N days.
     */
    @Transactional(readOnly = true)
    public TeamPulseAggregate getTeamAggregate(UserDetailsImpl viewer, UUID teamId, int daysBack) {
        if (!isTeamMember(viewer.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);
        List<PulseSurvey> surveys = fetchTeamSurveys(teamId, since);

        Team team = resolveTeam(teamId);
        Double avg = surveys.stream()
                .filter(s -> s.getEnergyScore() != null)
                .mapToInt(s -> s.getEnergyScore())
                .average()
                .orElse(Double.NaN);

        List<String> themes = surveys.stream()
                .map(PulseSurvey::getBurdenNote)
                .filter(b -> b != null && !b.isBlank())
                .toList();

        return new TeamPulseAggregate(
                teamId,
                team.getName(),
                Double.isNaN(avg) ? null : avg,
                surveys.size(),
                themes
        );
    }

    /**
     * Energy trend line for a team, grouped by week or month.
     */
    @Transactional(readOnly = true)
    public PulseTrendResponse getTeamTrend(UserDetailsImpl viewer, UUID teamId, String granularity, int daysBack) {
        if (!isTeamMember(viewer.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);
        List<PulseSurvey> surveys = fetchTeamSurveys(teamId, since);

        boolean byMonth = "month".equalsIgnoreCase(granularity);
        Map<LocalDate, List<Short>> buckets = new TreeMap<>();

        for (PulseSurvey s : surveys) {
            if (s.getEnergyScore() == null) continue;
            LocalDate date = s.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate bucket = byMonth ? date.withDayOfMonth(1)
                    : date.with(WeekFields.ISO.dayOfWeek(), 1L);
            buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(s.getEnergyScore());
        }

        List<PulseTrendPoint> points = buckets.entrySet().stream()
                .map(e -> {
                    List<Short> scores = e.getValue();
                    double avg = scores.stream().mapToInt(Short::intValue).average().orElse(0.0);
                    return new PulseTrendPoint(e.getKey(), avg, scores.size());
                })
                .toList();

        Team team = resolveTeam(teamId);
        return new PulseTrendResponse(teamId, team.getName(), granularity, points);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<PulseSurvey> fetchTeamSurveys(UUID teamId, Instant since) {
        var memberships = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged()).getContent();
        List<UUID> userIds = memberships.stream().map(m -> m.getUser().getId()).toList();
        // Fetch all surveys for these users created after 'since'
        return pulseSurveyRepository.findAll().stream()
                .filter(s -> userIds.contains(s.getUser().getId()) && s.getCreatedAt().isAfter(since))
                .toList();
    }

    private Team resolveTeam(UUID teamId) {
        return teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged())
                .getContent().stream().findFirst()
                .map(m -> m.getTeam())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
    }

    private boolean canAccessSurvey(UserDetailsImpl viewer, PulseSurvey survey) {
        if (viewer.getId().equals(survey.getUser().getId())) return true;
        return isTeamLeadForUser(viewer.getId(), survey.getUser().getId()) || isAdmin(viewer);
    }

    private boolean canModifySurvey(UserDetailsImpl viewer, PulseSurvey survey) {
        if (viewer.getId().equals(survey.getUser().getId())) return true;
        return isAdmin(viewer);
    }

    private boolean canAccessUserSurveys(UserDetailsImpl viewer, UUID userId) {
        if (viewer.getId().equals(userId)) return true;
        return isTeamLeadForUser(viewer.getId(), userId) || isAdmin(viewer);
    }

    private boolean isTeamLeadForUser(UUID leadId, UUID userId) {
        var memberships = teamMembershipRepository.findAllByUserId(userId, Pageable.unpaged()).getContent();
        for (var m : memberships) {
            UUID teamId = m.getTeam().getId();
            if (teamMembershipRepository
                    .findByUserIdAndTeamIdAndRole(leadId, teamId, UserRole.TEAM_LEAD)
                    .isPresent()
                || teamMembershipRepository
                    .findByUserIdAndTeamIdAndRole(leadId, teamId, UserRole.IT_LEAD)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isTeamMember(UUID userId, UUID teamId) {
        return teamMembershipRepository.findAllByUserId(userId, Pageable.unpaged()).getContent()
                .stream()
                .anyMatch(m -> m.getTeam().getId().equals(teamId));
    }

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private PulseSurveyResponse toResponse(PulseSurvey s) {
        return new PulseSurveyResponse(
                s.getId(),
                s.getUser().getId(),
                fullName(s.getUser()),
                s.getEnergyScore(),
                s.getBurdenNote(),
                s.getTopics(),
                s.getCreatedAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
