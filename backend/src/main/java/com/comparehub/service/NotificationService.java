package com.comparehub.service;

import com.comparehub.dto.NotificationCreateRequestDto;
import com.comparehub.dto.NotificationListResponseDto;
import com.comparehub.dto.NotificationResponseDto;

public interface NotificationService {

    NotificationResponseDto sendNotification(NotificationCreateRequestDto request);

    NotificationListResponseDto getUserNotifications(Long userId);

    NotificationResponseDto markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
