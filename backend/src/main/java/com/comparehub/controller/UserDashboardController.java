package com.comparehub.controller;

import com.comparehub.dto.UserDashboardResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.UserDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    @GetMapping
    public ResponseEntity<UserDashboardResponseDto> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request for dashboard by user ID: {}", userPrincipal.getId());
        UserDashboardResponseDto dashboard = userDashboardService.getDashboardData(userPrincipal.getId());
        return ResponseEntity.ok(dashboard);
    }
}
