package com.teamui.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Action item shown in the "Needs Attention" / Todos widget.
 *
 * @param id       action UUID
 * @param text     action description
 * @param ownerId  owner user id
 * @param dueDate  optional due date
 * @param overdue  true if past due date and not done
 * @param source   where the action came from (e.g. "1:1 with Lead")
 * @author TeamUI
 * @since 0.0.1
 */
public record QuickActionDto(
        UUID id,
        String text,
        UUID ownerId,
        LocalDate dueDate,
        boolean overdue,
        String source
) {}
