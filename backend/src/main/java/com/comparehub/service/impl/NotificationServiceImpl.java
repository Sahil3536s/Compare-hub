package com.comparehub.service.impl;

import com.comparehub.dto.NotificationCreateRequestDto;
import com.comparehub.dto.NotificationListResponseDto;
import com.comparehub.dto.NotificationResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.Notification;
import com.comparehub.model.User;
import com.comparehub.notification.channel.NotificationChannel;
import com.comparehub.repository.NotificationRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;

    @Override
    @Transactional
    public NotificationResponseDto sendNotification(NotificationCreateRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .read(false)
                .createdAt(Instant.now())
                .metadata(request.getMetadata())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Dispatch through enabled channels (InApp, Email, Push)
        for (NotificationChannel channel : notificationChannels) {
            try {
                if (channel.isEnabled()) {
                    channel.send(saved);
                }
            } catch (Exception e) {
                log.error("Channel '{}' error while sending notification to user {}: {}",
                        channel.getChannelName(), user.getId(), e.getMessage());
            }
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDto getUserNotifications(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<NotificationResponseDto> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        return NotificationListResponseDto.builder()
                .unreadCount(unreadCount)
                .notifications(list)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponseDto mapToDto(Notification n) {
        return NotificationResponseDto.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.getRead())
                .createdAt(n.getCreatedAt())
                .metadata(n.getMetadata())
                .build();
    }
}
