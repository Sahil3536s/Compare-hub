package com.comparehub.dto;

import com.comparehub.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean read;
    private Instant createdAt;
    private String metadata;
}
