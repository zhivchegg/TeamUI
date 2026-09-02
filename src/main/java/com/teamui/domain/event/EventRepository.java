package com.teamui.domain.event;

import com.teamui.domain.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link Event}. Uses {@link EntityGraph} to eagerly load
 * related users and teams.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"user", "createdBy", "team"})
    Page<Event> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "createdBy", "team"})
    Page<Event> findAllByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    Page<Event> findAllByUserIdAndEventTypeOrderByCreatedAtDesc(
            UUID userId, EventType eventType, Pageable pageable);

    Page<Event> findAllByTeamIdAndEventTypeOrderByCreatedAtDesc(
            UUID teamId, EventType eventType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "createdBy", "team"})
    Page<Event> findAllByUserIdAndVisibleToEmployeeTrueOrderByCreatedAtDesc(
            UUID userId, Pageable pageable);
}
