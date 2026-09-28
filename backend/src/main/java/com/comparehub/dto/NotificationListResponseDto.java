package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDto {

    private long unreadCount;
    @Builder.Default
    private List<NotificationResponseDto> notifications = new ArrayList<>();
}
