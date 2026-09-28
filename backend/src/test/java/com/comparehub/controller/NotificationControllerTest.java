package com.comparehub.controller;

import com.comparehub.dto.NotificationListResponseDto;
import com.comparehub.dto.NotificationResponseDto;
import com.comparehub.model.NotificationType;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnUserNotifications() throws Exception {
        NotificationResponseDto notification = NotificationResponseDto.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.PRICE_DROP)
                .title("iPhone 15 Price Drop")
                .message("Price dropped to ₹69,999")
                .read(false)
                .createdAt(Instant.now())
                .build();

        NotificationListResponseDto response = NotificationListResponseDto.builder()
                .unreadCount(1L)
                .notifications(List.of(notification))
                .build();

        when(notificationService.getUserNotifications(1L)).thenReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].title").value("iPhone 15 Price Drop"))
                .andExpect(jsonPath("$.notifications[0].type").value("PRICE_DROP"));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        NotificationResponseDto notification = NotificationResponseDto.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.PRICE_DROP)
                .title("iPhone 15 Price Drop")
                .read(true)
                .build();

        when(notificationService.markAsRead(1L, 1L)).thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/1/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));
    }
}
