package com.teamui.service;

import com.teamui.domain.cluster.Cluster;
import com.teamui.domain.cluster.ClusterRepository;
import com.teamui.domain.enums.UserRole;
import com.teamui.domain.stream.Stream;
import com.teamui.domain.stream.StreamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leadership above the team level: streams and clusters.
 *
 * <p>The organisation chain is Stream, then Cluster, then Team. Leads of streams and clusters
 * are stored on the {@code streams} and {@code clusters} tables, not in team memberships.
 * Team roles are resolved separately, from memberships.</p>
 *
 * <p>When a user leads both a stream and a cluster, the stream role is the primary one.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class LeadershipResolver {

    /**
     * Primary leadership of a user.
     *
     * @param role    {@code STREAM_LEAD}, {@code STREAM_IT_LEAD}, {@code CLUSTER_LEAD} or {@code CLUSTER_IT_LEAD}
     * @param stream  the stream for stream roles, otherwise null
     * @param cluster the cluster for cluster roles, otherwise null
     */
    public record Leadership(UserRole role, Stream stream, Cluster cluster) {}

    private final StreamRepository streamRepository;
    private final ClusterRepository clusterRepository;

    public LeadershipResolver(StreamRepository streamRepository, ClusterRepository clusterRepository) {
        this.streamRepository = streamRepository;
        this.clusterRepository = clusterRepository;
    }

    /**
     * Primary leadership of a user: stream roles first, then cluster roles.
     *
     * @param userId the user
     * @return the leadership, or empty if the user leads no stream and no cluster
     */
    public Optional<Leadership> find(UUID userId) {
        Optional<Leadership> asStream = streamRepository.findAllByStreamLeadId(userId).stream().findFirst()
                .map(s -> new Leadership(UserRole.STREAM_LEAD, s, null))
                .or(() -> streamRepository.findAllByStreamItLeadId(userId).stream().findFirst()
                        .map(s -> new Leadership(UserRole.STREAM_IT_LEAD, s, null)));
        if (asStream.isPresent()) {
            return asStream;
        }
        return clusterRepository.findAllByClusterLeadId(userId).stream().findFirst()
                .map(c -> new Leadership(UserRole.CLUSTER_LEAD, null, c))
                .or(() -> clusterRepository.findAllByClusterItLeadId(userId).stream().findFirst()
                        .map(c -> new Leadership(UserRole.CLUSTER_IT_LEAD, null, c)));
    }

    /**
     * Clusters this user leads, as cluster lead or IT lead.
     *
     * @param userId the user
     * @return cluster ids, empty if none
     */
    public Set<UUID> ledClusterIds(UUID userId) {
        return java.util.stream.Stream.concat(
                        clusterRepository.findAllByClusterLeadId(userId).stream(),
                        clusterRepository.findAllByClusterItLeadId(userId).stream())
                .map(Cluster::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Streams this user leads, as stream lead or IT lead.
     *
     * @param userId the user
     * @return stream ids, empty if none
     */
    public Set<UUID> ledStreamIds(UUID userId) {
        return java.util.stream.Stream.concat(
                        streamRepository.findAllByStreamLeadId(userId).stream(),
                        streamRepository.findAllByStreamItLeadId(userId).stream())
                .map(Stream::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Clusters this user leads, as cluster lead or IT lead.
     *
     * @param userId the user
     * @return clusters, empty if none
     */
    public List<Cluster> clustersLedBy(UUID userId) {
        return java.util.stream.Stream.concat(
                        clusterRepository.findAllByClusterLeadId(userId).stream(),
                        clusterRepository.findAllByClusterItLeadId(userId).stream())
                .toList();
    }
}
