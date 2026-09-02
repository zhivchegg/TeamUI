package com.teamui.domain.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for IT systems tracked in Bus Factor analysis.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface ITSystemRepository extends JpaRepository<ITSystem, UUID> {

    List<ITSystem> findAllByTeamId(UUID teamId);
}
