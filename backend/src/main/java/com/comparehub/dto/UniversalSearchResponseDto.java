package com.comparehub.dto;

import com.comparehub.model.SearchIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSearchResponseDto {

    private SearchIntent intent;
    private String query;
    private SearchIntentResultDto intentDetails;
    private String redirectRoute; // e.g. "/shopping", "/flights", "/rides"

    // Routed Domain Comparison Results
    private ProductComparisonResponseDto productResults;
    private FlightComparisonResponseDto flightResults;
    private RideComparisonResponseDto rideResults;

    // AI & Ranking Highlights
    private AiRecommendationDto aiRecommendation;
    private RankingSummaryDto rankingSummary;

    // Phase 32: NLU & Clarification
    private StructuredQueryUnderstandingDto queryUnderstanding;
    @Builder.Default
    private Boolean requiresClarification = false;
    @Builder.Default
    private java.util.List<String> missingFields = new java.util.ArrayList<>();
    private String clarificationPrompt;

    private long executionTimeMs;
}
