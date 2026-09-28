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
public class BudgetPlanOptionDto {

    private String id;
    private String title;
    private BigDecimal totalCost;
    private BigDecimal costPerTraveler;
    private BigDecimal remainingBudget;
    private double budgetUtilizationPercent;
    private int durationMinutes;
    private String formattedDuration;
    private int transferCount;
    private String comfortLevel; // "HIGH", "MEDIUM", "STANDARD"
    private String classification; // "FASTEST", "BEST_VALUE", "CHEAPEST", "COMFORT_PICK", "FEASIBLE_MATCH"
    private String recommendationReason;
    private JourneyOptionDto journeyOption;
    private boolean isWithinBudget;
    private List<String> highlights;
}
