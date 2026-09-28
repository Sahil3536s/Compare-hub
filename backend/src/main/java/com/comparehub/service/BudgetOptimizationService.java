package com.comparehub.service;

import com.comparehub.dto.BudgetConstraint;
import com.comparehub.dto.BudgetOptimizationResponseDto;

public interface BudgetOptimizationService {

    BudgetOptimizationResponseDto optimizeBudgetPlan(BudgetConstraint constraint);

    BudgetConstraint parseNaturalLanguageQuery(String query);

    BudgetOptimizationResponseDto getSampleBudgetPlan();
}
