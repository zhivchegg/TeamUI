package com.teamui.domain.competency;

import com.teamui.domain.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual score for a user against a specific competency axis.
 *
 * <p>Each score may have a self-assessment ({@code selfScore}) and a lead assessment
 * ({@code leadScore}) with an optional comment.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "user_competency_scores",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "competency_id"})
       })
public class UserCompetencyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competency_id", nullable = false)
    private Competency competency;

    @Column(name = "self_score")
    private Short selfScore;

    @Column(name = "lead_score")
    private Short leadScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scored_by")
    private User scoredBy;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt = Instant.now();

    /** JPA default constructor. */
    protected UserCompetencyScore() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Competency getCompetency() { return competency; }
    public void setCompetency(Competency competency) { this.competency = competency; }

    public Short getSelfScore() { return selfScore; }
    public void setSelfScore(Short selfScore) { this.selfScore = selfScore; }

    public Short getLeadScore() { return leadScore; }
    public void setLeadScore(Short leadScore) { this.leadScore = leadScore; }

    public User getScoredBy() { return scoredBy; }
    public void setScoredBy(User scoredBy) { this.scoredBy = scoredBy; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }
}
