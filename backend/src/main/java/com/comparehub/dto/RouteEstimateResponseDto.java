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
public class RouteEstimateResponseDto {

    private Double distanceKm;
    private Integer durationMinutes;
    private String pickupAddress;
    private String dropAddress;
    @Builder.Default
    private List<List<Double>> polylineCoordinates = new ArrayList<>(); // [[lat, lon], ...]
}
