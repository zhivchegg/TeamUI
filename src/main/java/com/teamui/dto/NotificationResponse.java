package com.teamui.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One in-app notification.
 *
 * @param id        notification UUID
 * @param text      message shown to the user
 * @param meetingId meeting the notification is about, null if the meeting was deleted
 * @param read      true if the user has seen it
 * @param createdAt when the notification was created
 * @author TeamUI
 * @since 0.0.1
 */
public record NotificationResponse(
        UUID id,
        String text,
        UUID meetingId,
        boolean read,
        Instant createdAt
) {}
