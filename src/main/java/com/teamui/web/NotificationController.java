package com.teamui.web;

import com.teamui.dto.ApiResponse;
import com.teamui.dto.NotificationResponse;
import com.teamui.dto.UnreadCountResponse;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for in-app notifications of the caller.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Notifications of the caller, newest first. */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(user.getId(), pageable));
    }

    /** Number of unread notifications, shown as a badge in the header. */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCount(user.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable UUID id) {
        notificationService.markRead(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
