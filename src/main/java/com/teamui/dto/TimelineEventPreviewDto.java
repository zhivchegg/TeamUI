package com.teamui.dto;

import com.teamui.domain.enums.EventType;
import com.teamui.domain.enums.ImpactLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact timeline event for the dashboard preview.
 *
 * @param id          event UUID
 * @param eventType   achievement / incident / note / training / etc.
 * @param title       event headline
 * @param category    optional sub-category
 * @param impactLevel positive / negative magnitude
 * @param createdAt   when the event was recorded
 * @author TeamUI
 * @since 0.0.1
 */
public record TimelineEventPreviewDto(
        UUID id,
        EventType eventType,
        String title,
        String category,
        ImpactLevel impactLevel,
        Instant createdAt
) {}
