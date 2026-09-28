package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyOptionDto {

    private String id;
    private String title;
    private List<JourneySegmentDto> segments;
    private BigDecimal totalCost;
    private int totalDurationMinutes;
    private String formattedTotalDuration;
    private int totalWaitingTimeMinutes;
    private int transferCount;
    private int travelerCount;
    private BigDecimal costPerTraveler;
    private double costScore;
    private double timeScore;
    private double compositeScore;
    private String classification; // "CHEAPEST", "FASTEST", "BALANCED", "TOP_MATCH"
    private String recommendationReason;
    private String timelineSummary;
    private boolean isCheapest;
    private boolean isFastest;
    private boolean isBalanced;
    private List<String> insights;
}
