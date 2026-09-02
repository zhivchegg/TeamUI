package com.teamui.dto;

import java.util.List;
import java.util.UUID;

/**
 * Benchmark view overlaying an employee's radar on top of the team average.
 *
 * @param userId     employee UUID
 * @param userName   employee full name
 * @param teamId     team UUID
 * @param teamName   team name
 * @param employee   employee radar points
 * @param team       team average radar points
 * @param deltas     per-axis differences (employee avg − team avg)
 * @author TeamUI
 * @since 0.0.1
 */
public record BenchmarkResponse(
        UUID userId,
        String userName,
        UUID teamId,
        String teamName,
        List<CompetencyRadarPointDto> employee,
        List<CompetencyRadarPointDto> team,
        List<Double> deltas
) {}
