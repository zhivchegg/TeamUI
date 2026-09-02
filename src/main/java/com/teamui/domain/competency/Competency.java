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
}
