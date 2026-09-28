package com.comparehub.dto;

import com.comparehub.model.BudgetPreference;
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
public class UnifiedDecisionRequest {

    private String query;
    private String decisionType; // "AUTO_DETECT", "PRODUCT", "CART", "FLIGHT", "RIDE", "JOURNEY", "BUDGET", "GROUP_TRAVEL"
    
    // User Context & Preferences
    private Long userId;
    private String preference; // "CHEAPEST", "FASTEST", "BALANCED", "BEST_VALUE", "RATING", "COMFORT"
    
    // Travel & Journey Parameters
    private String origin;
    private String destination;
    private String travelDate;
    private Integer travelers;
    private BigDecimal maxBudget;
    private Integer airportBufferMinutes;
    private String timePreference; // "ANY", "MORNING", "AFTERNOON", "EVENING"
    private Integer maxStops;

    // Cart Parameters
    @Builder.Default
    private List<CartItemDto> cartItems = new ArrayList<>();
}
