package com.teamui.domain.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for teams (smallest organisational unit).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * List all teams inside a given cluster.
     *
     * @param clusterId the cluster identifier
     * @return teams ordered by name
     */
    List<Team> findAllByClusterId(UUID clusterId);
}
