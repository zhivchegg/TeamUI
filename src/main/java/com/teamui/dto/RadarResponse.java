package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Star-radar competency view for an employee.
 *
 * @param userId   employee UUID
 * @param userName employee full name
 * @param points   one point per competency axis (sorted by sortOrder)
 * @author TeamUI
 * @since 0.0.1
 */
public record RadarResponse(
        UUID userId,
        String userName,
        List<CompetencyRadarPointDto> points
) {}
