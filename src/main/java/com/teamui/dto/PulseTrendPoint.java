package com.teamui.dto;

import java.time.LocalDate;

/**
 * Single point on the team energy trend line.
 *
 * @param period      bucket start date (week or month)
 * @param avgEnergy   average energy score in this bucket
 * @param responseCount how many surveys contributed
 * @author TeamUI
 * @since 0.0.1
 */
public record PulseTrendPoint(
        LocalDate period,
        Double avgEnergy,
        int responseCount
) {}
