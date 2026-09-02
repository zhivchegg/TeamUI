package com.teamui.domain.meeting;

import com.teamui.domain.enums.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for action items created during 1:1 meetings.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface MeetingActionRepository extends JpaRepository<MeetingAction, UUID> {

    List<MeetingAction> findAllByMeetingId(UUID meetingId);

    Page<MeetingAction> findAllByOwnerIdOrderByDueDateAsc(UUID ownerId, Pageable pageable);

    Page<MeetingAction> findAllByOwnerIdAndStatusOrderByDueDateAsc(
            UUID ownerId, ActionStatus status, Pageable pageable);

    /**
     * Find overdue or soon-due actions for an owner.
     *
     * @param ownerId  the action owner
     * @param statuses statuses to include (typically OPEN, IN_PROGRESS)
     * @param dueDate  the cutoff date
     * @param pageable pagination parameters
     * @return ordered by due date ascending
     */
    Page<MeetingAction> findAllByOwnerIdAndStatusInAndDueDateBeforeOrderByDueDateAsc(
            UUID ownerId, List<ActionStatus> statuses, LocalDate dueDate, Pageable pageable);
}
