package com.teamui.domain.competency;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Competency axis for the star-radar model.
 *
 * <p>Seven competencies are seeded via Flyway for support teams,
 * including the "AI and Automation" axis introduced in 2026.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Entity
@Table(name = "competencies")
public class Competency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** Team the competency belongs to, null for company-wide competencies. */
    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "level_1", columnDefinition = "TEXT")
    private String level1;

    @Column(name = "level_2", columnDefinition = "TEXT")
    private String level2;

    @Column(name = "level_3", columnDefinition = "TEXT")
    private String level3;

    @Column(name = "level_4", columnDefinition = "TEXT")
    private String level4;

    /** JPA default constructor. */
    public Competency() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getLevel1() { return level1; }
    public void setLevel1(String level1) { this.level1 = level1; }

    public String getLevel2() { return level2; }
    public void setLevel2(String level2) { this.level2 = level2; }

    public String getLevel3() { return level3; }
    public void setLevel3(String level3) { this.level3 = level3; }

    public String getLevel4() { return level4; }
    public void setLevel4(String level4) { this.level4 = level4; }
}
