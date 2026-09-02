package com.teamui.dto;

import java.time.LocalDate;

/**
 * Single point on a competency trend line.
 *
 * @param period    the bucket start date (week or month)
 * @param avgScore  average score during this period (0.0 if no data)
 * @param count     how many snapshots contributed
 * @author TeamUI
 * @since 0.0.1
 */
public record TrendPointDto(
        LocalDate period,
        double avgScore,
        int count
) {}
