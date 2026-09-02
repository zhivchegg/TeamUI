package com.teamui.domain.pulse;

import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Quick self-assessment filled by an employee before a 1:1.
 *
 * <p>Captures energy level, current burdens, and topics the employee wants to discuss.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "pulse_surveys")
public class PulseSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "energy_score")
    private Short energyScore;

    @Column(name = "burden_note", columnDefinition = "TEXT")
    private String burdenNote;

    @Column(columnDefinition = "TEXT")
    private String topics;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA default constructor. */
    protected PulseSurvey() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Short getEnergyScore() { return energyScore; }
    public void setEnergyScore(Short energyScore) { this.energyScore = energyScore; }

    public String getBurdenNote() { return burdenNote; }
    public void setBurdenNote(String burdenNote) { this.burdenNote = burdenNote; }

    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
