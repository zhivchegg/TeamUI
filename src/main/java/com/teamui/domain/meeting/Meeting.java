package com.teamui.domain.meeting;

import com.teamui.domain.enums.MeetingStatus;
import com.teamui.domain.team.Team;
import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A 1:1 meeting between a lead and a participant.
 *
 * <p>{@code sharedNotes} are editable only by the lead and visible to both leads
 * of the team. {@code privateNotes} are visible only to the meeting author ({@code lead}).</p>
 *
 * <p>Uses optimistic locking via {@code version} to prevent concurrent edits.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private User lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "scheduled_date", nullable = false)
    private Instant scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MeetingStatus status = MeetingStatus.PLANNED;

    @Column(name = "energy_score")
    private Short energyScore;

    @Column(name = "energy_note", columnDefinition = "TEXT")
    private String energyNote;

    @Column(name = "shared_notes", columnDefinition = "TEXT")
    private String sharedNotes;

    @Column(name = "private_notes", columnDefinition = "TEXT")
    private String privateNotes;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA default constructor. */
    protected Meeting() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    public User getLead() { return lead; }
    public void setLead(User lead) { this.lead = lead; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public Instant getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(Instant scheduledDate) { this.scheduledDate = scheduledDate; }

    public MeetingStatus getStatus() { return status; }
    public void setStatus(MeetingStatus status) { this.status = status; }

    public Short getEnergyScore() { return energyScore; }
    public void setEnergyScore(Short energyScore) { this.energyScore = energyScore; }

    public String getEnergyNote() { return energyNote; }
    public void setEnergyNote(String energyNote) { this.energyNote = energyNote; }

    public String getSharedNotes() { return sharedNotes; }
    public void setSharedNotes(String sharedNotes) { this.sharedNotes = sharedNotes; }

    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }

    public Long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
