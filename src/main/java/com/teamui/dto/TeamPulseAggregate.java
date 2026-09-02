package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated pulse data for a team over a time window.
 *
 * @param teamId        team UUID
 * @param teamName      team name
 * @param energyAverage average energy score (1–5, null if no data)
 * @param responseCount how many surveys contributed
 * @param burdenThemes  list of raw burden notes (for word-cloud / theming)
 * @author TeamUI
 * @since 0.0.1
 */
public record TeamPulseAggregate(
        UUID teamId,
        String teamName,
        Double energyAverage,
        int responseCount,
        List<String> burdenThemes
) {}
