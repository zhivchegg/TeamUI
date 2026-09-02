package com.teamui.domain.competency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for competency axes (star-radar model).
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {

    /**
     * Return all competencies in display order.
     *
     * @return ordered list
     */
    List<Competency> findAllByOrderBySortOrderAsc();
}
