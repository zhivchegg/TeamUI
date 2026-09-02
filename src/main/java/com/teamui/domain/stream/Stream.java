package com.teamui.domain.stream;

import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Top-level organisational unit. Contains clusters and teams.
 *
 * <p>Each stream has a business lead ({@code streamLead}) and an IT lead
 * ({@code streamItLead}). These roles are defined here, not via membership.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "streams")
public class Stream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String type = "SUPPORT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_lead_id")
    private User streamLead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_it_lead_id")
    private User streamItLead;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA default constructor. */
    public Stream() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public User getStreamLead() { return streamLead; }
    public void setStreamLead(User streamLead) { this.streamLead = streamLead; }

    public User getStreamItLead() { return streamItLead; }
    public void setStreamItLead(User streamItLead) { this.streamItLead = streamItLead; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
