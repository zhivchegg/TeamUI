package com.teamui.domain.cluster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for clusters (organisational unit below stream).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

    /**
     * List all clusters belonging to a given stream.
     *
     * @param streamId the stream identifier
     * @return clusters ordered by name
     */
    List<Cluster> findAllByStreamId(UUID streamId);
}
