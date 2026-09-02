package com.teamui.domain.event;

import com.teamui.domain.enums.EventType;
import com.teamui.domain.enums.ImpactLevel;
import com.teamui.domain.team.Team;
import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Timeline entry for an employee: achievements, incidents, notes, or training.
 *
 * <p>Created by a lead or by the system. {@code visibleToEmployee} controls whether
 * the entry is shown on the employee's own dashboard.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", length = 50)
    private ImpactLevel impactLevel;

    @Column(name = "is_visible_to_employee", nullable = false)
    private boolean visibleToEmployee = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA default constructor. */
    public Event() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ImpactLevel getImpactLevel() { return impactLevel; }
    public void setImpactLevel(ImpactLevel impactLevel) { this.impactLevel = impactLevel; }

    public boolean isVisibleToEmployee() { return visibleToEmployee; }
    public void setVisibleToEmployee(boolean visibleToEmployee) { this.visibleToEmployee = visibleToEmployee; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
