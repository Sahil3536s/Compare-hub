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
public class DecisionContext {

    private String contextType; // PRODUCT, FLIGHT, RIDE, SMART_JOURNEY, GROUP_TRAVEL, BUDGET_PLAN
    private String query;
    private String userPriority; // CHEAPEST, FASTEST, BEST_VALUE, RATING, COMFORT, BALANCED
    
    // Normalized comparison entities
    @Builder.Default
    private List<NormalizedProductOfferDto> productOffers = new ArrayList<>();
    @Builder.Default
    private List<NormalizedFlightOfferDto> flightOffers = new ArrayList<>();
    @Builder.Default
    private List<NormalizedRideOfferDto> rideOffers = new ArrayList<>();
    @Builder.Default
    private List<JourneyOptionDto> journeyOptions = new ArrayList<>();
    @Builder.Default
    private List<GroupTravelOptionDto> groupTravelOptions = new ArrayList<>();
    @Builder.Default
    private List<BudgetPlanOptionDto> budgetPlans = new ArrayList<>();

    // Quantitative metrics
    private String cheapestOption;
    private String bestOption;
    private String fastestOption;
    private BigDecimal priceDifference;
    private Integer timeSavedMinutes;
    private Double ratingDifference;

    // Additional context
    private String purchaseTimingStatus;
    private BigDecimal historicalAveragePrice;
    private BigDecimal historicalLowPrice;
    private String bestPaymentOfferDescription;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
