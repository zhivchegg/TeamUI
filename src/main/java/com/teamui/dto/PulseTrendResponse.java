package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Energy trend line for a team.
 *
 * @param teamId     team UUID
 * @param teamName   team name
 * @param granularity "week" or "month"
 * @param points     time-bucketed averages
 * @author TeamUI
 * @since 0.0.1
 */
public record PulseTrendResponse(
        UUID teamId,
        String teamName,
        String granularity,
        List<PulseTrendPoint> points
) {}
