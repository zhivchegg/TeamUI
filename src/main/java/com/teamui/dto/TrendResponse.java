package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Trend line for a single competency axis.
 *
 * @param userId         employee UUID
 * @param userName       employee full name
 * @param competencyId   competency axis
 * @param competencyName competency name
 * @param points         time-bucketed averages
 * @author TeamUI
 * @since 0.0.1
 */
public record TrendResponse(
        UUID userId,
        String userName,
        UUID competencyId,
        String competencyName,
        List<TrendPointDto> points
) {}
