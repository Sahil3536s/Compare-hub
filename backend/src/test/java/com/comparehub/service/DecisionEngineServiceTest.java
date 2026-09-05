package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.impl.DecisionEngineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private QueryUnderstandingService queryUnderstandingService;
    @Mock
    private ProductComparisonService productComparisonService;
    @Mock
    private PurchaseTimingService purchaseTimingService;
    @Mock
    private PaymentOfferService paymentOfferService;
    @Mock
    private ProductAlternativeService productAlternativeService;
    @Mock
    private CartOptimizationService cartOptimizationService;
    @Mock
    private FlightComparisonService flightComparisonService;
    @Mock
    private RideComparisonService rideComparisonService;
    @Mock
    private JourneyOptimizationService journeyOptimizationService;
    @Mock
    private BudgetOptimizationService budgetOptimizationService;
    @Mock
    private GroupTravelService groupTravelService;
    @Mock
    private DecisionAdvisorService decisionAdvisorService;

    private DecisionEngineService decisionEngineService;

    @BeforeEach
    void setUp() {
        decisionEngineService = new DecisionEngineServiceImpl(
                queryUnderstandingService,
                productComparisonService,
                purchaseTimingService,
                paymentOfferService,
                productAlternativeService,
                cartOptimizationService,
                flightComparisonService,
                rideComparisonService,
                journeyOptimizationService,
                budgetOptimizationService,
                groupTravelService,
                decisionAdvisorService
        );
    }

    @Test
    @DisplayName("Should orchestrate full Product decision pipeline")
    void testEvaluateProductDecision() {
        NormalizedProductOfferDto amazonOffer = NormalizedProductOfferDto.builder().merchant("Amazon").price(BigDecimal.valueOf(64999.00)).rating(4.6).inStock(true).build();
        NormalizedProductOfferDto cromaOffer = NormalizedProductOfferDto.builder().merchant("Croma").price(BigDecimal.valueOf(64299.00)).rating(4.1).inStock(true).build();

        when(productComparisonService.compareProducts(eq("iPhone 15"), any(), any(), any(), any(), any(), anyBoolean(), anyString())).thenReturn(
                ProductComparisonResponseDto.builder()
                        .query("iPhone 15")
                        .offers(List.of(amazonOffer, cromaOffer))
                        .build()
        );

        when(purchaseTimingService.evaluateOfferTiming(any())).thenReturn(
                PurchaseTimingDto.builder().status("GOOD_TIME_TO_BUY").score(82).build()
        );

        when(paymentOfferService.getUserPreferences(anyLong())).thenReturn(
                PaymentPreferenceDto.builder().preferredBank("HDFC").preferredCardType("Credit").build()
        );

        when(paymentOfferService.evaluatePaymentOffers(any(), any(), any(), any(), any())).thenReturn(
                PaymentOfferSummaryDto.builder()
                        .bestEligiblePrice(BigDecimal.valueOf(61999.00))
                        .bestOffer(PaymentOfferDto.builder().title("HDFC Credit").discountAmount(BigDecimal.valueOf(3000)).build())
                        .build()
        );

        when(productAlternativeService.getAlternatives(anyString(), any(), any(), any(), anyInt())).thenReturn(
                ProductAlternativesResponseDto.builder().alternatives(List.of()).build()
        );

        when(decisionAdvisorService.adviseProduct(anyList(), anyString())).thenReturn(
                DecisionRecommendation.builder()
                        .recommendedOption("Amazon (₹64,999)")
                        .summary("Amazon offers top overall value.")
                        .reasons(List.of("4.6 rating"))
                        .tradeoffs(List.of("Price premium"))
                        .confidence("HIGH")
                        .build()
        );

        UnifiedDecisionResponse response = decisionEngineService.evaluate(
                UnifiedDecisionRequest.builder().decisionType("PRODUCT").query("iPhone 15").build());

        assertNotNull(response);
        assertEquals("PRODUCT", response.getDecisionType());
        assertNotNull(response.getCheapest());
        assertNotNull(response.getDecisionAdvisor());
        assertEquals("Amazon (₹64,999)", response.getDecisionAdvisor().getRecommendedOption());
        assertFalse(response.getPipelineAudit().isEmpty());
    }

    @Test
    @DisplayName("Should orchestrate Flight decision pipeline")
    void testEvaluateFlightDecision() {
        when(flightComparisonService.compareFlights(any())).thenReturn(
                FlightComparisonResponseDto.builder()
                        .offers(List.of(
                                NormalizedFlightOfferDto.builder().airline("IndiGo").flightNumber("6E-204").price(BigDecimal.valueOf(5450.00)).durationMinutes(135).stops(0).isBest(true).build(),
                                NormalizedFlightOfferDto.builder().airline("SpiceJet").flightNumber("SG-819").price(BigDecimal.valueOf(4850.00)).durationMinutes(255).stops(1).isBest(false).build()
                        ))
                        .build()
        );

        when(decisionAdvisorService.adviseFlight(anyList(), anyString())).thenReturn(
                DecisionRecommendation.builder().recommendedOption("IndiGo 6E-204 (₹5,450)").summary("IndiGo saves time.").build()
        );

        UnifiedDecisionResponse response = decisionEngineService.evaluate(
                UnifiedDecisionRequest.builder().decisionType("FLIGHT").origin("DEL").destination("BOM").build());

        assertNotNull(response);
        assertEquals("FLIGHT", response.getDecisionType());
        assertNotNull(response.getCheapest());
        assertNotNull(response.getFastest());
    }

    @Test
    @DisplayName("Should orchestrate Budget constraint pipeline")
    void testEvaluateBudgetDecision() {
        when(budgetOptimizationService.optimizeBudgetPlan(any())).thenReturn(
                BudgetOptimizationResponseDto.builder()
                        .plans(List.of(
                                BudgetPlanOptionDto.builder().id("p1").title("Option B - Best Value").totalCost(BigDecimal.valueOf(17200.00)).remainingBudget(BigDecimal.valueOf(7800.00)).costPerTraveler(BigDecimal.valueOf(5733.33)).build()
                        ))
                        .bestPlan(BudgetPlanOptionDto.builder().id("p1").title("Option B - Best Value").totalCost(BigDecimal.valueOf(17200.00)).remainingBudget(BigDecimal.valueOf(7800.00)).costPerTraveler(BigDecimal.valueOf(5733.33)).build())
                        .build()
        );

        when(decisionAdvisorService.adviseBudgetPlan(anyList(), any())).thenReturn(
                DecisionRecommendation.builder().recommendedOption("Option B (₹17,200)").summary("Optimal balance leaving ₹7,800 reserve.").build()
        );

        UnifiedDecisionResponse response = decisionEngineService.evaluate(
                UnifiedDecisionRequest.builder().decisionType("BUDGET").maxBudget(BigDecimal.valueOf(25000.00)).travelers(3).build());

        assertNotNull(response);
        assertEquals("BUDGET", response.getDecisionType());
        assertNotNull(response.getBudgetOptimization());
        assertEquals(BigDecimal.valueOf(7800.00), response.getEstimatedSavings());
    }

    @Test
    @DisplayName("Should auto-detect intent from Natural Language query")
    void testAutoDetectQuery() {
        when(queryUnderstandingService.understandQuery("Flights from Delhi to Mumbai")).thenReturn(
                StructuredQueryUnderstandingDto.builder().intent(SearchIntent.FLIGHT_SEARCH).confidence(0.96).build()
        );

        when(flightComparisonService.compareFlights(any())).thenReturn(
                FlightComparisonResponseDto.builder().offers(List.of()).build()
        );

        when(decisionAdvisorService.adviseFlight(anyList(), anyString())).thenReturn(
                DecisionRecommendation.builder().recommendedOption("Flight advice").build()
        );

        UnifiedDecisionResponse response = decisionEngineService.evaluate(
                UnifiedDecisionRequest.builder().query("Flights from Delhi to Mumbai").decisionType("AUTO_DETECT").build());

        assertNotNull(response);
        assertEquals("FLIGHT", response.getDecisionType());
        assertNotNull(response.getInterpretedIntent());
    }
}
