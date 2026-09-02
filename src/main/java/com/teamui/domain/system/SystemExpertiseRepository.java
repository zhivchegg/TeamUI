package com.teamui.domain.system;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for system expertise links (Bus Factor matrix).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface SystemExpertiseRepository extends JpaRepository<SystemExpertise, UUID> {

    Page<SystemExpertise> findAllBySystemId(UUID systemId, Pageable pageable);

    Page<SystemExpertise> findAllByUserId(UUID userId, Pageable pageable);

    Optional<SystemExpertise> findBySystemIdAndUserId(UUID systemId, UUID userId);
}
