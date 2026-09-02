package com.teamui.dto;

import java.util.UUID;

/**
 * Single point on the star-radar competency chart.
 *
 * @param competencyId   competency UUID
 * @param name           competency name
 * @param selfScore      employee self-assessment (1-5) or null
 * @param leadScore      lead assessment (1-5) or null
 * @param average        computed average of available scores
 * @author TeamUI
 * @since 0.0.1
 */
public record CompetencyRadarPointDto(
        UUID competencyId,
        String name,
        Short selfScore,
        Short leadScore,
        double average
) {}
