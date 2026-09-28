package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;
import com.comparehub.service.impl.CostTimeOptimizationServiceImpl;
import com.comparehub.service.impl.JourneyOptimizationServiceImpl;
import com.comparehub.service.impl.BudgetOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetOptimizationServiceTest {

    private BudgetOptimizationService budgetOptimizationService;
    private JourneyOptimizationService journeyOptimizationService;
    private CostTimeOptimizationService costTimeOptimizationService;

    @BeforeEach
    void setUp() {
        costTimeOptimizationService = new CostTimeOptimizationServiceImpl();
        journeyOptimizationService = new JourneyOptimizationServiceImpl(costTimeOptimizationService);
        budgetOptimizationService = new BudgetOptimizationServiceImpl(journeyOptimizationService);
    }

    @Test
    @DisplayName("Should parse natural language query into structured BudgetConstraint")
    void testParseNaturalLanguageQuery() {
        String query = "We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.";
        BudgetConstraint constraint = budgetOptimizationService.parseNaturalLanguageQuery(query);

        assertNotNull(constraint);
        assertEquals(3, constraint.getTravelers());
        assertEquals("Delhi", constraint.getOrigin());
        assertEquals("Goa", constraint.getDestination());
        assertEquals(BigDecimal.valueOf(25000.0), constraint.getMaxBudget());
        assertEquals(BudgetPreference.BALANCED, constraint.getPreference());
    }

    @Test
    @DisplayName("Should prune travel options exceeding budget and calculate remaining reserves")
    void testOptimizeBudgetPlan() {
        BudgetConstraint constraint = BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(25000.00))
                .preference(BudgetPreference.BALANCED)
                .build();

        BudgetOptimizationResponseDto response = budgetOptimizationService.optimizeBudgetPlan(constraint);

        assertNotNull(response);
        assertNotNull(response.getPlans());
        assertFalse(response.getPlans().isEmpty());

        // All returned plans must strictly be <= maxBudget
        for (BudgetPlanOptionDto plan : response.getPlans()) {
            assertTrue(plan.getTotalCost().compareTo(BigDecimal.valueOf(25000.00)) <= 0);
            assertTrue(plan.getRemainingBudget().compareTo(BigDecimal.ZERO) >= 0);
            assertEquals(plan.getTotalCost().add(plan.getRemainingBudget()), BigDecimal.valueOf(25000.00));
        }

        // Check key options
        assertNotNull(response.getCheapestPlan());
        assertNotNull(response.getFastestPlan());
        assertNotNull(response.getBestValuePlan());

        assertTrue(response.getSummaryText().contains("Budget: ₹25,000"));
    }

    @Test
    @DisplayName("Should gracefully handle tight budget without inventing fake prices")
    void testTightBudgetHandling() {
        BudgetConstraint tightConstraint = BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(2000.00)) // Far too low for 3 flights/cabs
                .build();

        BudgetOptimizationResponseDto response = budgetOptimizationService.optimizeBudgetPlan(tightConstraint);

        assertNotNull(response);
        assertTrue(response.getPlans().isEmpty());
        assertTrue(response.getExceededBudgetOptionsCount() > 0);
        assertTrue(response.getLowestAvailablePrice().compareTo(BigDecimal.valueOf(2000.00)) > 0);
        assertTrue(response.getSummaryText().contains("No available travel combinations found"));
    }
}
