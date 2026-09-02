package com.teamui.domain.system;

import com.teamui.domain.enums.ExpertiseLevel;
import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps an employee's expertise level to a specific system (Bus Factor matrix).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "system_expertise",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"system_id", "user_id"})
       })
public class SystemExpertise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private ITSystem system;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExpertiseLevel level;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA default constructor. */
    public SystemExpertise() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ITSystem getSystem() { return system; }
    public void setSystem(ITSystem system) { this.system = system; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ExpertiseLevel getLevel() { return level; }
    public void setLevel(ExpertiseLevel level) { this.level = level; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
