package com.teamui.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary card for a single team member on the lead dashboard.
 *
 * @param id                 user UUID
 * @param fullName           combined first + last name
 * @param avatarUrl          optional avatar
 * @param role               their role in this team
 * @param lastOneOnOneDate   date of most recent 1:1
 * @param openActionCount    how many actions they own
 * @param competencyAverage  overall competency average (1-5)
 * @author TeamUI
 * @since 0.0.1
 */
public record TeamMemberCardDto(
        UUID id,
        String fullName,
        String avatarUrl,
        String role,
        Instant lastOneOnOneDate,
        long openActionCount,
        Double competencyAverage
) {}
