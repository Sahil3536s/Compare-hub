package com.comparehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedRouteDto {

    private Long id;
    @NotBlank(message = "Pickup location is required")
    private String pickup;
    @NotBlank(message = "Destination is required")
    private String destination;
    private String rideType;
    private Instant createdAt;
}
