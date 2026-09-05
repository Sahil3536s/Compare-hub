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
public class FlightComparisonResponseDto {

    private String origin;
    private String destination;
    private String departureDate;
    private String returnDate;
    private Integer totalOffers;
    private BigDecimal cheapestPrice;
    private Integer fastestDurationMinutes;
    private String bestAirline;
    @Builder.Default
    private List<NormalizedFlightOfferDto> offers = new ArrayList<>();
    private AiRecommendationDto aiRecommendation;
    private RankingSummaryDto rankingSummary;
}
