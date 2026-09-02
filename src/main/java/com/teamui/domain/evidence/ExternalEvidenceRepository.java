package com.teamui.domain.evidence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for external evidence (Git, Jira, etc).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface ExternalEvidenceRepository extends JpaRepository<ExternalEvidence, UUID> {

    Page<ExternalEvidence> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<ExternalEvidence> findAllByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);
}
