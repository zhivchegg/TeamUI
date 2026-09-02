package com.teamui.domain.competency;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for competency score history snapshots.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface UserCompetencyScoreHistoryRepository extends JpaRepository<UserCompetencyScoreHistory, UUID> {

    @EntityGraph(attributePaths = {"user", "competency", "scoredBy"})
    Page<UserCompetencyScoreHistory> findAllByUserIdOrderByScoredAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "competency", "scoredBy"})
    Page<UserCompetencyScoreHistory> findAllByUserIdAndCompetencyIdOrderByScoredAtDesc(
            UUID userId, UUID competencyId, Pageable pageable);
}
