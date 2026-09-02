package com.teamui.service;

import com.teamui.domain.competency.*;
import com.teamui.domain.enums.UserRole;
import com.teamui.domain.membership.TeamMembershipRepository;
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
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Business logic for the star-radar competency model.
 *
 * <p>Supports self-assessment by employees and lead assessment of team members.
 * The radar view synthesises self and lead scores into a single chart.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class CompetencyService {

    private final CompetencyRepository competencyRepository;
    private final UserCompetencyScoreRepository scoreRepository;
    private final UserCompetencyScoreHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public CompetencyService(CompetencyRepository competencyRepository,
                             UserCompetencyScoreRepository scoreRepository,
                             UserCompetencyScoreHistoryRepository historyRepository,
                             UserRepository userRepository,
                             TeamMembershipRepository teamMembershipRepository) {
        this.competencyRepository = competencyRepository;
        this.scoreRepository = scoreRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    // ------------------------------------------------------------------
    // Competency axes
    // ------------------------------------------------------------------

    /**
     * Return all competency axes in display order.
     */
    @Transactional(readOnly = true)
    public List<Competency> listCompetencies() {
        return competencyRepository.findAllByOrderBySortOrderAsc();
    }

    // ------------------------------------------------------------------
    // CRUD scores
    // ------------------------------------------------------------------

    /**
     * List scores for a user (both self and lead ratings).
     */
    @Transactional(readOnly = true)
    public Page<CompetencyScoreResponse> getScoresForUser(UserDetailsImpl viewer,
                                                             UUID targetUserId,
                                                             Pageable pageable) {
        if (!canViewScores(viewer, targetUserId)) {
            throw new AccessDeniedException("Not allowed to view this employee's scores");
        }

        return scoreRepository.findAllByUserId(targetUserId, pageable)
                .map(s -> toScoreResponse(s, viewer));
    }

    /**
     * Submit or update a self-assessment.
     */
    @Transactional
    public CompetencyScoreResponse submitSelfScore(UserDetailsImpl user,
                                                    UUID targetUserId,
                                                    ScoreSelfRequest request) {
        if (!canActOnBehalf(user, targetUserId)) {
            throw new AccessDeniedException("Not allowed to submit self-score for this user");
        }

        UserCompetencyScore score = findOrCreateScore(targetUserId, request.competencyId());
        score.setSelfScore(request.selfScore());
        if (request.comment() != null) {
            score.setComment(request.comment().trim());
        }
        score.setScoredAt(Instant.now());

        UserCompetencyScore saved = scoreRepository.save(score);
        snapshotHistory(saved);
        return toScoreResponse(saved, user);
    }

    /**
     * Submit or update a lead assessment.
     */
    @Transactional
    public CompetencyScoreResponse submitLeadScore(UserDetailsImpl lead,
                                                    UUID targetUserId,
                                                    ScoreLeadRequest request) {
        if (!isTeamLeadForUser(lead.getId(), targetUserId) && !isAdmin(lead)) {
            throw new AccessDeniedException("Only the employee's team lead may submit lead scores");
        }

        UserCompetencyScore score = findOrCreateScore(targetUserId, request.competencyId());
        score.setLeadScore(request.leadScore());
        score.setScoredBy(lead.getUser());
        if (request.comment() != null) {
            score.setComment(request.comment().trim());
        }
        score.setScoredAt(Instant.now());

        UserCompetencyScore saved = scoreRepository.save(score);
        snapshotHistory(saved);
        return toScoreResponse(saved, lead);
    }

    // ------------------------------------------------------------------
    // Radar
    // ------------------------------------------------------------------

    /**
     * Build a star-radar view for an employee.
     */
    @Transactional(readOnly = true)
    public RadarResponse getRadar(UserDetailsImpl viewer, UUID targetUserId) {
        if (!canViewScores(viewer, targetUserId)) {
            throw new AccessDeniedException("Not allowed to view this employee's radar");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Competency> competencies = competencyRepository.findAllByOrderBySortOrderAsc();

        List<CompetencyRadarPointDto> points = competencies.stream()
                .map(c -> {
                    Optional<UserCompetencyScore> opt = scoreRepository
                            .findByUserIdAndCompetencyId(targetUserId, c.getId());

                    Short self = opt.map(UserCompetencyScore::getSelfScore).orElse(null);
                    Short lead = opt.map(UserCompetencyScore::getLeadScore).orElse(null);
                    double avg = computeAverage(self, lead);

                    return new CompetencyRadarPointDto(
                            c.getId(), c.getName(), self, lead, avg
                    );
                })
                .toList();

        return new RadarResponse(targetUserId, fullName(user), points);
    }

    /**
     * Build a team-average star-radar.
     */
    @Transactional(readOnly = true)
    public TeamRadarResponse getTeamRadar(UserDetailsImpl viewer, UUID teamId) {
        if (!isTeamMember(viewer.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        com.teamui.domain.team.Team team = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged())
                .getContent().stream().findFirst()
                .map(m -> m.getTeam())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        List<Competency> competencies = competencyRepository.findAllByOrderBySortOrderAsc();

        // Collect all active members of the team
        var memberships = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged()).getContent();

        List<CompetencyRadarPointDto> points = competencies.stream()
                .map(c -> {
                    double totalSelf = 0;
                    double totalLead = 0;
                    int selfCount = 0;
                    int leadCount = 0;

                    for (var m : memberships) {
                        UUID memberId = m.getUser().getId();
                        var opt = scoreRepository.findByUserIdAndCompetencyId(memberId, c.getId());
                        if (opt.isPresent()) {
                            UserCompetencyScore s = opt.get();
                            if (s.getSelfScore() != null) {
                                totalSelf += s.getSelfScore();
                                selfCount++;
                            }
                            if (s.getLeadScore() != null) {
                                totalLead += s.getLeadScore();
                                leadCount++;
                            }
                        }
                    }

                    Short avgSelf = selfCount > 0 ? (short) Math.round(totalSelf / selfCount) : null;
                    Short avgLead = leadCount > 0 ? (short) Math.round(totalLead / leadCount) : null;
                    double avg = computeAverage(avgSelf, avgLead);

                    return new CompetencyRadarPointDto(c.getId(), c.getName(), avgSelf, avgLead, avg);
                })
                .toList();

        return new TeamRadarResponse(teamId, team.getName(), points);
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    /**
     * List historical snapshots for a user, optionally filtered by competency.
     */
    @Transactional(readOnly = true)
    public Page<CompetencyScoreHistoryResponse> getScoreHistory(UserDetailsImpl viewer,
                                                                 UUID targetUserId,
                                                                 UUID competencyId,
                                                                 Pageable pageable) {
        if (!canViewScores(viewer, targetUserId)) {
            throw new AccessDeniedException("Not allowed to view this employee's history");
        }

        Page<UserCompetencyScoreHistory> page;
        if (competencyId != null) {
            page = historyRepository.findAllByUserIdAndCompetencyIdOrderByScoredAtDesc(targetUserId, competencyId, pageable);
        } else {
            page = historyRepository.findAllByUserIdOrderByScoredAtDesc(targetUserId, pageable);
        }
        return page.map(h -> toHistoryResponse(h, viewer));
    }

    // ------------------------------------------------------------------
    // Helpers — permissions
    // ------------------------------------------------------------------

    private boolean canViewScores(UserDetailsImpl viewer, UUID targetUserId) {
        if (isAdmin(viewer)) return true;
        if (viewer.getId().equals(targetUserId)) return true;
        return isTeamLeadForUser(viewer.getId(), targetUserId);
    }

    private boolean canActOnBehalf(UserDetailsImpl viewer, UUID targetUserId) {
        if (isAdmin(viewer)) return true;
        if (viewer.getId().equals(targetUserId)) return true;
        return isTeamLeadForUser(viewer.getId(), targetUserId);
    }

    private boolean isTeamLeadForUser(UUID leadId, UUID userId) {
        // Find all teams the target user belongs to
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

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isTeamMember(UUID userId, UUID teamId) {
        return teamMembershipRepository.findAllByUserId(userId, Pageable.unpaged()).getContent()
                .stream()
                .anyMatch(m -> m.getTeam().getId().equals(teamId));
    }

    // ------------------------------------------------------------------
    // Helpers — data
    // ------------------------------------------------------------------

    private UserCompetencyScore findOrCreateScore(UUID userId, UUID competencyId) {
        return scoreRepository.findByUserIdAndCompetencyId(userId, competencyId)
                .orElseGet(() -> {
                    UserCompetencyScore s = new UserCompetencyScore();
                    User u = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    Competency c = competencyRepository.findById(competencyId)
                            .orElseThrow(() -> new IllegalArgumentException("Competency not found"));
                    s.setUser(u);
                    s.setCompetency(c);
                    return s;
                });
    }

    private double computeAverage(Short self, Short lead) {
        if (self != null && lead != null) {
            return (self + lead) / 2.0;
        }
        if (self != null) return self.doubleValue();
        if (lead != null) return lead.doubleValue();
        return 0.0;
    }

    private CompetencyScoreResponse toScoreResponse(UserCompetencyScore s, UserDetailsImpl viewer) {
        User scoredBy = s.getScoredBy();
        return new CompetencyScoreResponse(
                s.getId(),
                s.getUser().getId(),
                fullName(s.getUser()),
                s.getCompetency().getId(),
                s.getCompetency().getName(),
                s.getSelfScore(),
                s.getLeadScore(),
                scoredBy != null ? scoredBy.getId() : null,
                scoredBy != null ? fullName(scoredBy) : null,
                s.getComment(),
                s.getScoredAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    /**
     * Snapshot the current score into history table.
     */
    private void snapshotHistory(UserCompetencyScore score) {
        UserCompetencyScoreHistory h = new UserCompetencyScoreHistory();
        h.setUser(score.getUser());
        h.setCompetency(score.getCompetency());
        h.setSelfScore(score.getSelfScore());
        h.setLeadScore(score.getLeadScore());
        h.setScoredBy(score.getScoredBy());
        h.setComment(score.getComment());
        h.setScoredAt(score.getScoredAt());
        historyRepository.save(h);
    }

    private CompetencyScoreHistoryResponse toHistoryResponse(UserCompetencyScoreHistory h,
                                                              UserDetailsImpl viewer) {
        User scoredBy = h.getScoredBy();
        return new CompetencyScoreHistoryResponse(
                h.getId(),
                h.getUser().getId(),
                fullName(h.getUser()),
                h.getCompetency().getId(),
                h.getCompetency().getName(),
                h.getSelfScore(),
                h.getLeadScore(),
                scoredBy != null ? scoredBy.getId() : null,
                scoredBy != null ? fullName(scoredBy) : null,
                h.getComment(),
                h.getScoredAt()
        );
    }

    // ------------------------------------------------------------------
    // Trend line
    // ------------------------------------------------------------------

    /**
     * Build a trend line for a user's competency, grouped by week or month.
     *
     * @param granularity "week" or "month"
     */
    @Transactional(readOnly = true)
    public TrendResponse getTrend(UserDetailsImpl viewer,
                                   UUID targetUserId,
                                   UUID competencyId,
                                   String granularity) {
        if (!canViewScores(viewer, targetUserId)) {
            throw new AccessDeniedException("Not allowed to view this employee's trend");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Competency competency = competencyRepository.findById(competencyId)
                .orElseThrow(() -> new IllegalArgumentException("Competency not found"));

        List<UserCompetencyScoreHistory> history = historyRepository
                .findAllByUserIdAndCompetencyIdOrderByScoredAtDesc(targetUserId, competencyId, Pageable.unpaged())
                .getContent();

        boolean byMonth = "month".equalsIgnoreCase(granularity);

        Map<LocalDate, List<Short>> buckets = new TreeMap<>();
        for (UserCompetencyScoreHistory h : history) {
            LocalDate date = h.getScoredAt().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate bucket = byMonth
                    ? date.withDayOfMonth(1)
                    : date.with(WeekFields.ISO.getFirstDayOfWeek(), 1);

            Short effective = h.getLeadScore() != null ? h.getLeadScore() : h.getSelfScore();
            if (effective != null) {
                buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(effective);
            }
        }

        List<TrendPointDto> points = buckets.entrySet().stream()
                .map(e -> {
                    List<Short> scores = e.getValue();
                    double avg = scores.stream().mapToInt(Short::intValue).average().orElse(0.0);
                    return new TrendPointDto(e.getKey(), avg, scores.size());
                })
                .toList();

        return new TrendResponse(targetUserId, fullName(user), competencyId, competency.getName(), points);
    }

    // ------------------------------------------------------------------
    // Benchmark
    // ------------------------------------------------------------------

    /**
     * Overlay an employee radar on top of the team average.
     */
    @Transactional(readOnly = true)
    public BenchmarkResponse getBenchmark(UserDetailsImpl viewer,
                                           UUID targetUserId,
                                           UUID teamId) {
        if (!canViewScores(viewer, targetUserId)) {
            throw new AccessDeniedException("Not allowed to view this benchmark");
        }
        if (!isTeamMember(viewer.getId(), teamId)) {
            throw new AccessDeniedException("Not a member of this team");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        com.teamui.domain.team.Team team = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged())
                .getContent().stream().findFirst()
                .map(m -> m.getTeam())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        List<Competency> competencies = competencyRepository.findAllByOrderBySortOrderAsc();

        // Employee radar
        List<CompetencyRadarPointDto> employeePoints = competencies.stream()
                .map(c -> {
                    var opt = scoreRepository.findByUserIdAndCompetencyId(targetUserId, c.getId());
                    Short self = opt.map(UserCompetencyScore::getSelfScore).orElse(null);
                    Short lead = opt.map(UserCompetencyScore::getLeadScore).orElse(null);
                    double avg = computeAverage(self, lead);
                    return new CompetencyRadarPointDto(c.getId(), c.getName(), self, lead, avg);
                })
                .toList();

        // Team radar (reuse logic but compute in-memory)
        var memberships = teamMembershipRepository.findAllByTeamId(teamId, Pageable.unpaged()).getContent();
        List<CompetencyRadarPointDto> teamPoints = competencies.stream()
                .map(c -> {
                    double totalSelf = 0, totalLead = 0;
                    int selfCount = 0, leadCount = 0;
                    for (var m : memberships) {
                        var opt = scoreRepository.findByUserIdAndCompetencyId(m.getUser().getId(), c.getId());
                        if (opt.isPresent()) {
                            UserCompetencyScore s = opt.get();
                            if (s.getSelfScore() != null) { totalSelf += s.getSelfScore(); selfCount++; }
                            if (s.getLeadScore() != null) { totalLead += s.getLeadScore(); leadCount++; }
                        }
                    }
                    Short avgSelf = selfCount > 0 ? (short) Math.round(totalSelf / selfCount) : null;
                    Short avgLead = leadCount > 0 ? (short) Math.round(totalLead / leadCount) : null;
                    double avg = computeAverage(avgSelf, avgLead);
                    return new CompetencyRadarPointDto(c.getId(), c.getName(), avgSelf, avgLead, avg);
                })
                .toList();

        // Delta per axis
        List<Double> deltas = new ArrayList<>();
        for (int i = 0; i < competencies.size(); i++) {
            deltas.add(employeePoints.get(i).average() - teamPoints.get(i).average());
        }

        return new BenchmarkResponse(
                targetUserId,
                fullName(user),
                teamId,
                team.getName(),
                employeePoints,
                teamPoints,
                deltas
        );
    }
}
