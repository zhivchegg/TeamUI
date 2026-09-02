package com.teamui.domain.competency;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user competency assessments.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface UserCompetencyScoreRepository extends JpaRepository<UserCompetencyScore, UUID> {

    Page<UserCompetencyScore> findAllByUserId(UUID userId, Pageable pageable);

    Optional<UserCompetencyScore> findByUserIdAndCompetencyId(UUID userId, UUID competencyId);
}
