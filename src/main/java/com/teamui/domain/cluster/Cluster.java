package com.teamui.domain.cluster;

import com.teamui.domain.stream.Stream;
import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Organisation unit below {@link Stream}. Contains teams.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "clusters")
public class Cluster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private Stream stream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_lead_id")
    private User clusterLead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_it_lead_id")
    private User clusterItLead;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA default constructor. */
    public Cluster() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Stream getStream() { return stream; }
    public void setStream(Stream stream) { this.stream = stream; }

    public User getClusterLead() { return clusterLead; }
    public void setClusterLead(User clusterLead) { this.clusterLead = clusterLead; }

    public User getClusterItLead() { return clusterItLead; }
    public void setClusterItLead(User clusterItLead) { this.clusterItLead = clusterItLead; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
