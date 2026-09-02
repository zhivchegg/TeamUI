package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Team-average star-radar view.
 *
 * @param teamId   team UUID
 * @param teamName team name
 * @param points   one point per competency axis (average of all team members)
 * @author TeamUI
 * @since 0.0.1
 */
public record TeamRadarResponse(
        UUID teamId,
        String teamName,
        List<CompetencyRadarPointDto> points
) {}
