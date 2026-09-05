package com.comparehub.service;

import com.comparehub.dto.NotificationCreateRequestDto;
import com.comparehub.dto.NotificationListResponseDto;
import com.comparehub.dto.NotificationResponseDto;
import com.comparehub.model.Notification;
import com.comparehub.model.NotificationType;
import com.comparehub.model.User;
import com.comparehub.notification.channel.NotificationChannel;
import com.comparehub.repository.NotificationRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationChannel inAppNotificationChannel;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                List.of(inAppNotificationChannel)
        );
    }

    @Test
    void shouldSendNotificationAndDispatchToChannels() {
        User user = User.builder().id(1L).email("user@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Notification saved = Notification.builder()
                .id(10L)
                .user(user)
                .type(NotificationType.PRICE_DROP)
                .title("Price Drop Alert!")
                .message("iPhone 15 dropped to ₹69,999")
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        when(inAppNotificationChannel.isEnabled()).thenReturn(true);

        NotificationCreateRequestDto request = NotificationCreateRequestDto.builder()
                .userId(1L)
                .type(NotificationType.PRICE_DROP)
                .title("Price Drop Alert!")
                .message("iPhone 15 dropped to ₹69,999")
                .build();

        NotificationResponseDto response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(NotificationType.PRICE_DROP, response.getType());
        assertFalse(response.getRead());

        // Verify channel dispatch
        verify(inAppNotificationChannel, times(1)).send(saved);
    }

    @Test
    void shouldGetUserNotificationsWithUnreadCount() {
        User user = User.builder().id(1L).build();
        when(userRepository.existsById(1L)).thenReturn(true);

        Notification n1 = Notification.builder().id(1L).user(user).type(NotificationType.PRICE_DROP).title("Drop").message("msg").read(false).createdAt(Instant.now()).build();
        Notification n2 = Notification.builder().id(2L).user(user).type(NotificationType.SYSTEM).title("Welcome").message("msg").read(true).createdAt(Instant.now()).build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(1L);

        NotificationListResponseDto response = notificationService.getUserNotifications(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUnreadCount());
        assertEquals(2, response.getNotifications().size());
    }

    @Test
    void shouldMarkNotificationAsRead() {
        User user = User.builder().id(1L).build();
        Notification n = Notification.builder().id(1L).user(user).type(NotificationType.ALERT_REACHED).title("Alert").message("msg").read(false).build();

        when(notificationRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponseDto response = notificationService.markAsRead(1L, 1L);

        assertNotNull(response);
        assertTrue(response.getRead());
    }
}
