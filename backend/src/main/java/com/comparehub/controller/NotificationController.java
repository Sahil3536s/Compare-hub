package com.comparehub.controller;

import com.comparehub.dto.NotificationListResponseDto;
import com.comparehub.dto.NotificationResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponseDto> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        NotificationListResponseDto list = notificationService.getUserNotifications(userPrincipal.getId());
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id) {
        NotificationResponseDto updated = notificationService.markAsRead(userPrincipal.getId(), id);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
