package com.teamui.domain.meeting;

import com.teamui.domain.enums.MeetingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Meeting}. Uses {@link EntityGraph} to eagerly load
 * related users and teams in list queries, avoiding N+1 problems.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Page<Meeting> findAllByParticipantIdOrderByScheduledDateDesc(UUID participantId, Pageable pageable);

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Page<Meeting> findAllByLeadIdOrderByScheduledDateDesc(UUID leadId, Pageable pageable);

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Page<Meeting> findAllByTeamIdOrderByScheduledDateDesc(UUID teamId, Pageable pageable);

    List<Meeting> findAllByTeamIdAndStatus(UUID teamId, MeetingStatus status);

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Page<Meeting> findAllByParticipantIdAndLeadIdOrderByScheduledDateDesc(
            UUID participantId, UUID leadId, Pageable pageable);

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Optional<Meeting> findById(UUID id);

    @EntityGraph(attributePaths = {"participant", "lead", "team"})
    Optional<Meeting> findTopByParticipantIdAndStatusOrderByScheduledDateAsc(UUID participantId, MeetingStatus status);
}
