package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDecisionResponse {

    private String decisionType; // "PRODUCT", "CART", "FLIGHT", "RIDE", "JOURNEY", "BUDGET", "GROUP_TRAVEL"
    private String query;
    private StructuredQueryUnderstandingDto interpretedIntent;

    // Direct winning picks (for Products, Rides, Flights)
    private Object cheapest;
    private Object bestValue;
    private Object fastest;
    private Object recommended;

    // Travel & Door-to-Door Journey options
    private JourneyOptionDto cheapestJourney;
    private JourneyOptionDto fastestJourney;
    private JourneyOptionDto balancedJourney;
    private JourneyOptionDto recommendedJourney;

    // AI Decision Advisor 2.0 Explanation
    private DecisionRecommendation decisionAdvisor;

    // Financial & Savings Metrics
    private BigDecimal estimatedSavings;
    @Builder.Default
    private List<String> insights = new ArrayList<>();

    // Domain Specific Envelopes
    @Builder.Default
    private List<ProductAlternativeDto> alternatives = new ArrayList<>();
    private PurchaseTimingDto purchaseTiming;
    private PaymentOfferSummaryDto paymentOffers;
    private CartOptimizationResponseDto cartOptimization;
    private BudgetOptimizationResponseDto budgetOptimization;
    private GroupTravelResponseDto groupTravelOptimization;

    // Pipeline Execution Audit Trail
    @Builder.Default
    private List<String> pipelineAudit = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
