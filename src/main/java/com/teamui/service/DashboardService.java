package com.teamui.service;

import com.teamui.domain.cluster.Cluster;
import com.teamui.domain.cluster.ClusterRepository;
import com.teamui.domain.competency.Competency;
import com.teamui.domain.competency.CompetencyRepository;
import com.teamui.domain.competency.UserCompetencyScore;
import com.teamui.domain.competency.UserCompetencyScoreRepository;
import com.teamui.domain.enums.*;
import com.teamui.domain.event.Event;
import com.teamui.domain.event.EventRepository;
import com.teamui.domain.meeting.Meeting;
import com.teamui.domain.meeting.MeetingAction;
import com.teamui.domain.meeting.MeetingActionRepository;
import com.teamui.domain.meeting.MeetingRepository;
import com.teamui.domain.membership.TeamMembership;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.pulse.PulseSurvey;
import com.teamui.domain.pulse.PulseSurveyRepository;
import com.teamui.domain.stream.Stream;
import com.teamui.domain.stream.StreamRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds personalized dashboards based on the authenticated user's role.
 *
 * <p>Delivers different views for TEAM_MEMBER, TEAM_LEAD, STREAM_LEAD, and ADMIN.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final TeamMembershipRepository membershipRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingActionRepository actionRepository;
    private final EventRepository eventRepository;
    private final CompetencyRepository competencyRepository;
    private final UserCompetencyScoreRepository scoreRepository;
    private final PulseSurveyRepository pulseRepository;
    private final TeamRepository teamRepository;
    private final ClusterRepository clusterRepository;
    private final StreamRepository streamRepository;
    private final ITSystemRepository systemRepository;
    private final SystemExpertiseRepository expertiseRepository;

    public DashboardService(UserRepository userRepository,
                             TeamMembershipRepository membershipRepository,
                             MeetingRepository meetingRepository,
                             MeetingActionRepository actionRepository,
                             EventRepository eventRepository,
                             CompetencyRepository competencyRepository,
                             UserCompetencyScoreRepository scoreRepository,
                             PulseSurveyRepository pulseRepository,
                             TeamRepository teamRepository,
                             ClusterRepository clusterRepository,
                             StreamRepository streamRepository,
                             ITSystemRepository systemRepository,
                             SystemExpertiseRepository expertiseRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.meetingRepository = meetingRepository;
        this.actionRepository = actionRepository;
        this.eventRepository = eventRepository;
        this.competencyRepository = competencyRepository;
        this.scoreRepository = scoreRepository;
        this.pulseRepository = pulseRepository;
        this.teamRepository = teamRepository;
        this.clusterRepository = clusterRepository;
        this.streamRepository = streamRepository;
        this.systemRepository = systemRepository;
        this.expertiseRepository = expertiseRepository;
    }

    /**
     * Builds the complete dashboard for the authenticated user.
     */
    @Transactional(readOnly = true)
    public DashboardResponse buildDashboard(UserDetailsImpl userDetails) {
        UUID userId = userDetails.getId();
        User user = userRepository.findById(userId).orElseThrow();

        UserProfileDto profile = new UserProfileDto(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getAvatarUrl()
        );

        // Resolve membership and role
        var memberships = membershipRepository.findAllByUserId(userId, Pageable.ofSize(1));
        if (memberships.isEmpty()) {
            // Admin or stream-level role with no team membership
            return new DashboardResponse(
                    profile, UserRole.ADMIN, List.of(), null, null, null
            );
        }

        TeamMembership primaryMembership = memberships.getContent().getFirst();
        UserRole role = primaryMembership.getRole();
        Team team = primaryMembership.getTeam();

        List<QuickActionDto> quickActions = buildQuickActions(userId, role, team);

        MemberMetricsDto memberMetrics = null;
        TeamHealthDto teamHealth = null;
        StreamOverviewDto streamOverview = null;

        if (role == UserRole.TEAM_MEMBER) {
            memberMetrics = buildMemberMetrics(user, team);
        } else if (role == UserRole.TEAM_LEAD || role == UserRole.IT_LEAD) {
            teamHealth = buildTeamHealth(user, team);
            memberMetrics = buildMemberMetrics(user, team); // leads also see their own metrics
        } else if (role == UserRole.STREAM_LEAD || role == UserRole.STREAM_IT_LEAD) {
            streamOverview = buildStreamOverview(user, team);
        }

        return new DashboardResponse(profile, role, quickActions, memberMetrics, teamHealth, streamOverview);
    }

    private List<QuickActionDto> buildQuickActions(UUID userId, UserRole role, Team team) {
        List<QuickActionDto> actions = new ArrayList<>();

        // Actions owned by user
        Pageable soonPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "dueDate"));
        var ownedActions = actionRepository.findAllByOwnerIdAndStatusInAndDueDateBeforeOrderByDueDateAsc(
                userId,
                List.of(ActionStatus.OPEN, ActionStatus.IN_PROGRESS),
                LocalDate.now().plusDays(1),
                soonPageable
        );

        for (MeetingAction action : ownedActions) {
            actions.add(new QuickActionDto(
                    action.getId(), action.getText(), action.getOwner().getId(),
                    action.getDueDate(),
                    action.getDueDate() != null && action.getDueDate().isBefore(LocalDate.now()),
                    "1:1 Action"
            ));
        }

        return actions;
    }

    private MemberMetricsDto buildMemberMetrics(User user, Team team) {
        Pageable pageable = PageRequest.of(0, 3);

        // Competency radar
        List<Competency> competencies = competencyRepository.findAllByOrderBySortOrderAsc();
        List<UserCompetencyScore> scores = scoreRepository.findAllByUserId(user.getId(), Pageable.unpaged()).getContent();

        Map<UUID, UserCompetencyScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(s -> s.getCompetency().getId(), s -> s));

        List<CompetencyRadarPointDto> radar = competencies.stream().map(c -> {
            UserCompetencyScore sc = scoreMap.get(c.getId());
            Short self = sc != null ? sc.getSelfScore() : null;
            Short lead = sc != null ? sc.getLeadScore() : null;
            double avg = 0;
            int count = 0;
            if (self != null) { avg += self; count++; }
            if (lead != null) { avg += lead; count++; }
            return new CompetencyRadarPointDto(c.getId(), c.getName(), self, lead, count > 0 ? avg / count : 0);
        }).toList();

        // Last meetings
        var meetings = meetingRepository.findAllByParticipantIdOrderByScheduledDateDesc(user.getId(), pageable);
        List<MeetingPreviewDto> lastMeetings = meetings.getContent().stream()
                .filter(m -> m.getStatus() == MeetingStatus.COMPLETED)
                .map(this::toMeetingPreview)
                .toList();

        // Upcoming meeting
        var upcomingOpt = meetingRepository.findTopByParticipantIdAndStatusOrderByScheduledDateAsc(
                user.getId(), MeetingStatus.PLANNED);
        MeetingPreviewDto upcoming = upcomingOpt.map(this::toMeetingPreview).orElse(null);

        // Open actions
        var openActions = actionRepository.findAllByOwnerIdAndStatusOrderByDueDateAsc(
                user.getId(), ActionStatus.OPEN, pageable);
        List<QuickActionDto> actions = openActions.getContent().stream()
                .map(a -> new QuickActionDto(a.getId(), a.getText(), a.getOwner().getId(),
                        a.getDueDate(), false, "1:1 Action"))
                .toList();

        // Recent events
        var events = eventRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        List<TimelineEventPreviewDto> recentEvents = events.getContent().stream()
                .map(this::toTimelinePreview)
                .toList();

        return new MemberMetricsDto(radar, lastMeetings, upcoming, actions, recentEvents);
    }

    private TeamHealthDto buildTeamHealth(User lead, Team team) {
        Pageable pageable = PageRequest.of(0, 10);
        Instant now = Instant.now();
        Instant nextWeek = now.plus(7, ChronoUnit.DAYS);

        // Team members
        var members = membershipRepository.findAllByTeamId(team.getId(), Pageable.unpaged()).getContent();

        // Upcoming meetings in next 7 days
        List<Meeting> upcomingMeetings = meetingRepository.findAllByTeamIdOrderByScheduledDateDesc(
                team.getId(), Pageable.unpaged()).getContent().stream()
                .filter(m -> m.getScheduledDate().isAfter(now) && m.getScheduledDate().isBefore(nextWeek))
                .toList();

        // Overdue actions for team members
        var overdueActions = actionRepository.findAllByOwnerIdAndStatusInAndDueDateBeforeOrderByDueDateAsc(
                lead.getId(), List.of(ActionStatus.OPEN, ActionStatus.IN_PROGRESS), LocalDate.now(), pageable);
        // Note: in production this should query all team members' actions

        // Member cards
        List<TeamMemberCardDto> memberCards = members.stream().map(m -> {
            User member = m.getUser();
            var memberMeetings = meetingRepository.findAllByParticipantIdOrderByScheduledDateDesc(
                    member.getId(), PageRequest.of(0, 1));
            Instant lastMeeting = memberMeetings.hasContent()
                    ? memberMeetings.getContent().getFirst().getScheduledDate() : null;

            var memberScores = scoreRepository.findAllByUserId(member.getId(), Pageable.unpaged()).getContent();
            double avgScore = memberScores.stream()
                    .filter(s -> s.getLeadScore() != null)
                    .mapToInt(s -> s.getLeadScore())
                    .average().orElse(0);

            return new TeamMemberCardDto(
                    member.getId(), member.getFirstName() + " " + member.getLastName(),
                    member.getAvatarUrl(), m.getRole().name(), lastMeeting, 0, avgScore
            );
        }).toList();

        // Bus factor alerts
        List<BusFactorAlertDto> busAlerts = buildBusFactorAlerts(team);

        // Team energy average
        var pulses = pulseRepository.findAllByUserIdOrderByCreatedAtDesc(lead.getId(), Pageable.unpaged()).getContent();
        Double avgEnergy = pulses.stream()
                .filter(p -> p.getEnergyScore() != null)
                .mapToInt(p -> p.getEnergyScore())
                .average().orElse(0);
        if (avgEnergy == 0) avgEnergy = null;

        // Unacknowledged events (simplified: last 3 events)
        var events = eventRepository.findAllByTeamIdOrderByCreatedAtDesc(team.getId(), PageRequest.of(0, 3));
        List<TimelineEventPreviewDto> unackEvents = events.getContent().stream()
                .map(this::toTimelinePreview)
                .toList();

        return new TeamHealthDto(
                team.getName(),
                upcomingMeetings.stream().map(this::toMeetingPreview).toList(),
                overdueActions.getContent().stream()
                        .map(a -> new QuickActionDto(a.getId(), a.getText(), a.getOwner().getId(),
                                a.getDueDate(), true, "1:1 Action")).toList(),
                memberCards,
                busAlerts,
                avgEnergy,
                unackEvents
        );
    }

    private StreamOverviewDto buildStreamOverview(User user, Team team) {
        // Navigate up: team -> cluster -> stream
        Cluster cluster = team.getCluster();
        Stream stream = cluster.getStream();

        List<Cluster> clusters = clusterRepository.findAllByStreamId(stream.getId());

        int totalTeams = 0;
        int totalMembers = 0;
        int teamsAtRisk = 0;
        List<ClusterSummaryDto> clusterSummaries = new ArrayList<>();

        for (Cluster c : clusters) {
            List<Team> teams = teamRepository.findAllByClusterId(c.getId());
            int clusterTeamCount = teams.size();
            int clusterMemberCount = 0;
            long clusterOverdue = 0;
            long clusterBusIssues = 0;
            List<TeamMiniSummaryDto> teamSummaries = new ArrayList<>();

            for (Team t : teams) {
                var members = membershipRepository.findAllByTeamId(t.getId(), Pageable.unpaged()).getContent();
                int memberCount = members.size();
                clusterMemberCount += memberCount;
                totalMembers += memberCount;

                // Overdue actions (simplified: count OPEN/IN_PROGRESS past due)
                long overdue = 0; // Would need more complex query in production
                clusterOverdue += overdue;

                // Bus factor issues
                var systems = systemRepository.findAllByTeamId(t.getId());
                long busIssues = systems.stream().filter(s -> {
                    var expertises = expertiseRepository.findAllBySystemId(s.getId(), Pageable.unpaged()).getContent();
                    long experts = expertises.stream().filter(e -> e.getLevel() == ExpertiseLevel.EXPERT).count();
                    return experts == 0 || (experts == 1 && s.getCriticality().ordinal() <= Criticality.HIGH.ordinal());
                }).count();
                clusterBusIssues += busIssues;

                boolean atRisk = overdue > 0 || busIssues > 0;
                if (atRisk) teamsAtRisk++;

                teamSummaries.add(new TeamMiniSummaryDto(
                        t.getId(), t.getName(), memberCount, overdue, null, atRisk
                ));
            }

            totalTeams += clusterTeamCount;

            clusterSummaries.add(new ClusterSummaryDto(
                    c.getId(), c.getName(), clusterTeamCount, clusterMemberCount,
                    clusterOverdue, clusterBusIssues, teamSummaries
            ));
        }

        return new StreamOverviewDto(
                stream.getName(), clusterSummaries, totalTeams, totalMembers, teamsAtRisk
        );
    }

    private List<BusFactorAlertDto> buildBusFactorAlerts(Team team) {
        var systems = systemRepository.findAllByTeamId(team.getId());
        List<BusFactorAlertDto> alerts = new ArrayList<>();

        for (ITSystem sys : systems) {
            var expertises = expertiseRepository.findAllBySystemId(sys.getId(), Pageable.unpaged()).getContent();
            long experts = expertises.stream().filter(e -> e.getLevel() == ExpertiseLevel.EXPERT).count();
            long advanced = expertises.stream().filter(e -> e.getLevel() == ExpertiseLevel.ADVANCED).count();

            boolean atRisk = experts == 0 || (experts == 1 && sys.getCriticality().ordinal() <= Criticality.HIGH.ordinal());
            if (!atRisk) continue;

            List<String> expertNames = expertises.stream()
                    .filter(e -> e.getLevel() == ExpertiseLevel.EXPERT)
                    .map(e -> e.getUser().getFirstName() + " " + e.getUser().getLastName())
                    .toList();

            alerts.add(new BusFactorAlertDto(
                    sys.getId(), sys.getName(), sys.getCriticality(),
                    experts, advanced, expertises.size(), true, expertNames
            ));
        }

        return alerts;
    }

    private MeetingPreviewDto toMeetingPreview(Meeting meeting) {
        return new MeetingPreviewDto(
                meeting.getId(),
                meeting.getScheduledDate(),
                meeting.getStatus(),
                meeting.getParticipant().getFirstName() + " " + meeting.getParticipant().getLastName(),
                meeting.getLead().getFirstName() + " " + meeting.getLead().getLastName(),
                meeting.getEnergyScore()
        );
    }

    private TimelineEventPreviewDto toTimelinePreview(Event event) {
        return new TimelineEventPreviewDto(
                event.getId(),
                event.getEventType(),
                event.getTitle(),
                event.getCategory(),
                event.getImpactLevel(),
                event.getCreatedAt()
        );
    }
}
