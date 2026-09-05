package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartJourneyResponseDto {

    private String origin;
    private String destination;
    private int travelers;
    private int airportBufferMinutes;
    private double costWeight;
    private double timeWeight;
    private String tradeoffSummary;
    private JourneyOptionDto cheapestJourney;
    private JourneyOptionDto fastestJourney;
    private JourneyOptionDto balancedJourney;
    private JourneyOptionDto topRecommendedJourney;
    private List<JourneyOptionDto> allCombinations;
    private long executionTimeMs;
}
