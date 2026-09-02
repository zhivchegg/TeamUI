package com.teamui.web;

import com.teamui.dto.DashboardResponse;
import com.teamui.security.service.UserDetailsImpl;
import com.teamui.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard endpoint delivering a personalized view based on user role.
 *
 * <p>Requires authentication. Returns different payloads for:</p>
 * <ul>
 *   <li>TEAM_MEMBER — personal radar, upcoming 1:1, timeline preview</li>
 *   <li>TEAM_LEAD / IT_LEAD — team health, member cards, bus factor</li>
 *   <li>STREAM_LEAD / STREAM_IT_LEAD — aggregate across clusters</li>
 * </ul>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns the personalized dashboard for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return dashboard payload with role-appropriate data
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        DashboardResponse dashboard = dashboardService.buildDashboard(userDetails);
        return ResponseEntity.ok(dashboard);
    }
}
