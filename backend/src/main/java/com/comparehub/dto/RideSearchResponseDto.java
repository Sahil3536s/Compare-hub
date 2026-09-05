package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSearchResponseDto {
    private Long id;
    private Long userId;
    private String pickupLocation;
    private String dropLocation;
    private String rideType;
    private Instant createdAt;
}
