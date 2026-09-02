package com.teamui.domain.pulse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for pulse surveys (pre-1:1 check-ins).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface PulseSurveyRepository extends JpaRepository<PulseSurvey, UUID> {

    /**
     * Retrieve the check-in history for a specific user, newest first.
     *
     * @param userId   the user identifier
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated survey list
     */
    Page<PulseSurvey> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
