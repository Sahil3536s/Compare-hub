package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;
import com.comparehub.service.impl.DecisionAdvisorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionAdvisorServiceTest {

    private DecisionAdvisorService decisionAdvisorService;

    @BeforeEach
    void setUp() {
        decisionAdvisorService = new DecisionAdvisorServiceImpl();
    }

    @Test
    @DisplayName("Should advise on Product comparison with transparent tradeoffs and pricing difference")
    void testAdviseProduct() {
        List<NormalizedProductOfferDto> offers = List.of(
                NormalizedProductOfferDto.builder()
                        .merchant("Amazon")
                        .price(BigDecimal.valueOf(64999.00))
                        .rating(4.6)
                        .inStock(true)
                        .delivery("Tomorrow")
                        .discountPercent(12)
                        .build(),
                NormalizedProductOfferDto.builder()
                        .merchant("Croma")
                        .price(BigDecimal.valueOf(64299.00))
                        .rating(4.1)
                        .inStock(true)
                        .delivery("3-5 days")
                        .discountPercent(13)
                        .build()
        );

        DecisionRecommendation recommendation = decisionAdvisorService.adviseProduct(offers, "BALANCED");

        assertNotNull(recommendation);
        assertEquals("PRODUCT", recommendation.getContextType());
        assertTrue(recommendation.isDeterministic());
        assertNotNull(recommendation.getRecommendedOption());
        assertTrue(recommendation.getSummary().contains("Amazon"));
        assertTrue(recommendation.getSummary().contains("Croma"));
        assertFalse(recommendation.getReasons().isEmpty());
        assertFalse(recommendation.getTradeoffs().isEmpty());
        assertEquals("MEDIUM", recommendation.getConfidence());
    }

    @Test
    @DisplayName("Should advise on Flight comparison with stops, duration, and fare tradeoffs")
    void testAdviseFlight() {
        List<NormalizedFlightOfferDto> offers = List.of(
                NormalizedFlightOfferDto.builder()
                        .airline("IndiGo")
                        .flightNumber("6E-204")
                        .price(BigDecimal.valueOf(5450.00))
                        .durationMinutes(135)
                        .stops(0)
                        .isBest(true)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .airline("SpiceJet")
                        .flightNumber("SG-819")
                        .price(BigDecimal.valueOf(4850.00))
                        .durationMinutes(255)
                        .stops(1)
                        .isBest(false)
                        .build()
        );

        DecisionRecommendation recommendation = decisionAdvisorService.adviseFlight(offers, "BALANCED");

        assertNotNull(recommendation);
        assertEquals("FLIGHT", recommendation.getContextType());
        assertTrue(recommendation.getSummary().contains("6E-204"));
        assertTrue(recommendation.getSummary().contains("SG-819") || recommendation.getSummary().contains("SpiceJet"));
        assertFalse(recommendation.getTradeoffs().isEmpty());
    }

    @Test
    @DisplayName("Should advise on Budget Plan with reserve percentage and budget compliance")
    void testAdviseBudgetPlan() {
        List<BudgetPlanOptionDto> plans = List.of(
                BudgetPlanOptionDto.builder()
                        .id("plan-1")
                        .title("Balanced Transit")
                        .totalCost(BigDecimal.valueOf(17200.00))
                        .costPerTraveler(BigDecimal.valueOf(5733.33))
                        .remainingBudget(BigDecimal.valueOf(7800.00))
                        .budgetUtilizationPercent(68.8)
                        .durationMinutes(320)
                        .comfortLevel("STANDARD")
                        .formattedDuration("5h 20m")
                        .build()
        );

        DecisionRecommendation recommendation = decisionAdvisorService.adviseBudgetPlan(plans, BudgetPreference.BALANCED);

        assertNotNull(recommendation);
        assertEquals("BUDGET_PLAN", recommendation.getContextType());
        assertTrue(recommendation.getSummary().contains("Balanced Transit"));
        assertTrue(recommendation.getSummary().contains("17,200"));
        assertTrue(recommendation.getSummary().contains("7,800"));
    }

    @Test
    @DisplayName("Universal advise context dispatcher works seamlessly")
    void testUniversalAdvise() {
        DecisionContext context = DecisionContext.builder()
                .contextType("PRODUCT")
                .userPriority("CHEAPEST")
                .productOffers(List.of(
                        NormalizedProductOfferDto.builder()
                                .merchant("StoreA")
                                .price(BigDecimal.valueOf(100.00))
                                .rating(4.0)
                                .build(),
                        NormalizedProductOfferDto.builder()
                                .merchant("StoreB")
                                .price(BigDecimal.valueOf(120.00))
                                .rating(4.8)
                                .build()
                ))
                .build();

        DecisionRecommendation rec = decisionAdvisorService.advise(context);
        assertNotNull(rec);
        assertTrue(rec.getRecommendedOption().contains("StoreA"));
    }
}
