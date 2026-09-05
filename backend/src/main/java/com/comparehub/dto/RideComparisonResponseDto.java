package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideComparisonResponseDto {

    private LocationDto pickup;
    private LocationDto destination;
    private Double distanceKm;
    private Integer durationMinutes;
    private BigDecimal cheapestFare;
    private Integer fastestEtaMinutes;
    private String bestProvider;
    @Builder.Default
    private List<NormalizedRideOfferDto> offers = new ArrayList<>();
    private AiRecommendationDto aiRecommendation;
    private RankingSummaryDto rankingSummary;
}
