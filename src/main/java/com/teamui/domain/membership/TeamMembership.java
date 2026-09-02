package com.teamui.domain.membership;

import com.teamui.domain.enums.UserRole;
import com.teamui.domain.team.Team;
import com.teamui.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/**
 * Associates a {@link User} with a {@link Team} and defines their role.
 *
 * <p>Soft-delete is implemented via {@code leftAt}: when populated the membership
 * becomes inactive and is excluded from JPA queries via {@link SQLRestriction}.</p>
 *
 * <p>Note: only {@link UserRole#TEAM_MEMBER}, {@link UserRole#TEAM_LEAD} and
 * {@link UserRole#IT_LEAD} are valid at the DB level. Stream-level and ADMIN roles
 * are managed via separate fields on {@link com.teamui.domain.stream.Stream}.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "team_memberships",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "team_id", "role"})
       })
@SQLRestriction("left_at IS NULL")
public class TeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    /** JPA default constructor. */
    public TeamMembership() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }

    /**
     * Whether this membership is currently active.
     */
    public boolean isActive() {
        return leftAt == null;
    }
}
