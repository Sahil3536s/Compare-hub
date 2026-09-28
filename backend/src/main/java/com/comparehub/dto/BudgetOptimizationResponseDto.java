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
public class BudgetOptimizationResponseDto {

    private BudgetConstraint constraint;
    private BigDecimal maxBudget;
    private List<BudgetPlanOptionDto> plans;
    private BudgetPlanOptionDto bestPlan;
    private BudgetPlanOptionDto cheapestPlan;
    private BudgetPlanOptionDto fastestPlan;
    private BudgetPlanOptionDto bestValuePlan;
    private BudgetPlanOptionDto comfortPlan;
    private int exceededBudgetOptionsCount;
    private BigDecimal lowestAvailablePrice;
    private String summaryText;
    private long executionTimeMs;
}
