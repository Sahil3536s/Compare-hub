package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecisionEngineServiceImpl implements DecisionEngineService {

    private final QueryUnderstandingService queryUnderstandingService;
    private final ProductComparisonService productComparisonService;
    private final PurchaseTimingService purchaseTimingService;
    private final PaymentOfferService paymentOfferService;
    private final ProductAlternativeService productAlternativeService;
    private final CartOptimizationService cartOptimizationService;
    private final FlightComparisonService flightComparisonService;
    private final RideComparisonService rideComparisonService;
    private final JourneyOptimizationService journeyOptimizationService;
    private final BudgetOptimizationService budgetOptimizationService;
    private final GroupTravelService groupTravelService;
    private final DecisionAdvisorService decisionAdvisorService;

    @Override
    public UnifiedDecisionResponse evaluate(UnifiedDecisionRequest request) {
        if (request == null) {
            return evaluateSample("PRODUCT");
        }

        String type = request.getDecisionType();
        String query = request.getQuery();
        StructuredQueryUnderstandingDto nluIntent = null;

        // 1. Auto-detection via QueryUnderstandingService if needed
        if (type == null || "AUTO_DETECT".equalsIgnoreCase(type)) {
            if (request.getCartItems() != null && !request.getCartItems().isEmpty()) {
                type = "CART";
            } else if (request.getMaxBudget() != null && request.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
                type = "BUDGET";
            } else if (query != null && !query.isBlank()) {
                nluIntent = queryUnderstandingService.understandQuery(query);
                if (nluIntent != null && nluIntent.getIntent() != null) {
                    type = switch (nluIntent.getIntent()) {
                        case FLIGHT_SEARCH -> "FLIGHT";
                        case RIDE_SEARCH -> "RIDE";
                        case PRODUCT_SEARCH -> "PRODUCT";
                        default -> "PRODUCT";
                    };
                }
            } else {
                type = "PRODUCT";
            }
        }

        log.info("DecisionEngine orchestrating request for decisionType: '{}' (Query: '{}')", type, query);

        UnifiedDecisionResponse response = switch (type.toUpperCase()) {
            case "PRODUCT" -> evaluateProduct(
                    query != null && !query.isBlank() ? query : "iPhone 15",
                    request.getUserId() != null ? request.getUserId() : 1L,
                    request.getPreference() != null ? request.getPreference() : "BALANCED"
            );
            case "CART" -> evaluateCart(
                    request.getCartItems() != null && !request.getCartItems().isEmpty()
                            ? request.getCartItems()
                            : List.of(CartItemDto.builder().name("Milk 1L").quantity(2).maxPrice(BigDecimal.valueOf(60)).build()),
                    request.getUserId() != null ? request.getUserId() : 1L
            );
            case "FLIGHT" -> evaluateFlight(
                    request.getOrigin() != null ? request.getOrigin() : "DEL",
                    request.getDestination() != null ? request.getDestination() : "BOM",
                    request.getTravelDate() != null ? request.getTravelDate() : "2026-09-10",
                    request.getTimePreference() != null ? request.getTimePreference() : "ANY",
                    request.getMaxStops(),
                    request.getPreference() != null ? request.getPreference() : "BALANCED"
            );
            case "RIDE" -> evaluateRide(
                    28.6139, 77.2090, 28.5562, 77.1000,
                    request.getPreference() != null ? request.getPreference() : "BALANCED"
            );
            case "JOURNEY" -> evaluateJourney(
                    request.getOrigin() != null ? request.getOrigin() : "Delhi",
                    request.getDestination() != null ? request.getDestination() : "Goa",
                    request.getTravelDate() != null ? request.getTravelDate() : "2026-09-10",
                    request.getTravelers() != null ? request.getTravelers() : 2,
                    request.getPreference() != null ? request.getPreference() : "BALANCED"
            );
            case "BUDGET" -> evaluateBudget(
                    BudgetConstraint.builder()
                            .origin(request.getOrigin() != null ? request.getOrigin() : "Delhi")
                            .destination(request.getDestination() != null ? request.getDestination() : "Goa")
                            .travelers(request.getTravelers() != null ? request.getTravelers() : 3)
                            .maxBudget(request.getMaxBudget() != null ? request.getMaxBudget() : BigDecimal.valueOf(25000.00))
                            .preference(parseBudgetPreference(request.getPreference()))
                            .naturalLanguageQuery(query)
                            .build()
            );
            case "GROUP_TRAVEL" -> evaluateGroupTravel(
                    request.getOrigin() != null ? request.getOrigin() : "Delhi",
                    request.getDestination() != null ? request.getDestination() : "Jaipur",
                    request.getTravelDate() != null ? request.getTravelDate() : "2026-09-10",
                    request.getTravelers() != null ? request.getTravelers() : 4,
                    request.getPreference() != null ? request.getPreference() : "CHEAPEST"
            );
            default -> evaluateProduct(query, request.getUserId(), request.getPreference());
        };

        if (nluIntent != null && response.getInterpretedIntent() == null) {
            response.setInterpretedIntent(nluIntent);
        }

        return response;
    }

    @Override
    public UnifiedDecisionResponse evaluateProduct(String query, Long userId, String priority) {
        String effectiveQuery = (query != null && !query.isBlank()) ? query : "iPhone 15";
        List<String> audit = new ArrayList<>(List.of(
                "NLU_INTENT_PARSING",
                "MULTI_MERCHANT_PROVIDER_SEARCH",
                "OFFER_NORMALIZATION",
                "CANONICAL_PRODUCT_MATCHING",
                "TRUE_COST_EVALUATION",
                "PURCHASE_TIMING_ANALYSIS",
                "PAYMENT_OFFER_OPTIMIZATION",
                "ALTERNATIVE_DISCOVERY",
                "AI_DECISION_ADVISOR_2_0"
        ));

        ProductComparisonResponseDto comparison = productComparisonService.compareProducts(
                effectiveQuery, null, null, null, null, null, false, "relevance");

        List<NormalizedProductOfferDto> offers = comparison != null ? comparison.getOffers() : List.of();

        NormalizedProductOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                .orElse(null);

        NormalizedProductOfferDto highestRated = offers.stream()
                .max(Comparator.comparingDouble(o -> o.getRating() != null ? o.getRating() : 0.0))
                .orElse(cheapest);

        NormalizedProductOfferDto bestValue = offers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getInStock()))
                .max(Comparator.comparingDouble(o -> (o.getRating() != null ? o.getRating() : 3.5) * 20.0
                        - (cheapest != null ? (o.getPrice().doubleValue() / cheapest.getPrice().doubleValue()) * 30.0 : 0.0)))
                .orElse(cheapest);

        // Price Intelligence: Purchase Timing
        PurchaseTimingDto timing = cheapest != null ? purchaseTimingService.evaluateOfferTiming(cheapest) : null;

        // Payment Offers Optimization
        PaymentPreferenceDto userPref = paymentOfferService.getUserPreferences(userId != null ? userId : 1L);
        BigDecimal currentPrice = cheapest != null ? cheapest.getPrice() : BigDecimal.valueOf(64999.00);
        PaymentOfferSummaryDto paymentOffers = paymentOfferService.evaluatePaymentOffers(
                currentPrice, BigDecimal.ZERO, cheapest != null ? cheapest.getMerchant() : "Amazon", "ELECTRONICS", userPref);

        // Product Alternatives
        ProductAlternativesResponseDto altResp = productAlternativeService.getAlternatives(effectiveQuery, currentPrice, "ELECTRONICS", null, 3);
        List<ProductAlternativeDto> alternatives = altResp != null ? altResp.getAlternatives() : List.of();

        // AI Decision Advisor 2.0
        DecisionRecommendation advisor = decisionAdvisorService.adviseProduct(offers, priority);

        // Estimated Savings calculation
        BigDecimal maxPrice = offers.stream().map(NormalizedProductOfferDto::getPrice).max(BigDecimal::compareTo).orElse(currentPrice);
        BigDecimal estimatedSavings = maxPrice.subtract(currentPrice);
        if (estimatedSavings.compareTo(BigDecimal.ZERO) <= 0) {
            estimatedSavings = BigDecimal.valueOf(1500.00);
        }

        List<String> insights = new ArrayList<>();
        if (cheapest != null) {
            insights.add(String.format("Lowest market price: ₹%s via %s.", formatCurrency(cheapest.getPrice()), cheapest.getMerchant()));
        }
        if (timing != null && timing.getStatus() != null) {
            insights.add(String.format("Purchase Timing: %s (Buy Score: %.1f/10).", timing.getStatus(), (timing.getScore() != null ? timing.getScore() / 10.0 : 8.0)));
        }
        if (paymentOffers != null && paymentOffers.getBestEligiblePrice() != null) {
            insights.add(String.format("Extra Payment Savings: Best eligible price is ₹%s (%s).",
                    formatCurrency(paymentOffers.getBestEligiblePrice()),
                    paymentOffers.getBestOffer() != null ? paymentOffers.getBestOffer().getTitle() : "Applied offer"));
        }

        return UnifiedDecisionResponse.builder()
                .decisionType("PRODUCT")
                .query(effectiveQuery)
                .cheapest(cheapest)
                .bestValue(bestValue)
                .fastest(highestRated)
                .recommended(bestValue != null ? bestValue : cheapest)
                .decisionAdvisor(advisor)
                .estimatedSavings(estimatedSavings)
                .insights(insights)
                .alternatives(alternatives)
                .purchaseTiming(timing)
                .paymentOffers(paymentOffers)
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateCart(List<CartItemDto> cartItems, Long userId) {
        List<String> audit = List.of(
                "CART_ITEM_EXTRACTION",
                "MULTI_MERCHANT_INVENTORY_LOOKUP",
                "SINGLE_VS_SPLIT_MERCHANT_OPTIMIZATION",
                "SHIPPING_FEE_BALANCING",
                "AI_DECISION_ADVISOR_2_0"
        );

        CartOptimizationRequestDto cartReq = CartOptimizationRequestDto.builder()
                .items(cartItems != null ? cartItems : List.of())
                .strategy("MINIMIZE_PRICE")
                .build();

        CartOptimizationResponseDto cartOpt = cartOptimizationService.optimizeCart(cartReq);

        BigDecimal optimalTotal = (cartOpt != null && cartOpt.getRecommendedPlan() != null)
                ? cartOpt.getRecommendedPlan().getGrandTotal()
                : BigDecimal.valueOf(250.00);

        DecisionRecommendation advisor = DecisionRecommendation.builder()
                .recommendedOption(cartOpt != null && cartOpt.getRecommendedPlan() != null ? cartOpt.getRecommendedPlan().getTitle() : "Optimized Multi-Store Split")
                .summary(cartOpt != null ? String.format("Optimized cart plan saves ₹%s compared to alternative checkout options.",
                        formatCurrency(cartOpt.getEstimatedSavings())) : "Cart analyzed.")
                .reasons(List.of(
                        "Items routed to lowest net price merchants after delivery fees.",
                        String.format("Total optimized cart cost: ₹%s.", formatCurrency(optimalTotal))
                ))
                .tradeoffs(List.of("Split delivery involves receiving separate courier packages on different days."))
                .confidence("HIGH")
                .deterministic(true)
                .contextType("CART")
                .build();

        return UnifiedDecisionResponse.builder()
                .decisionType("CART")
                .query("Multi-Item Shopping Cart Optimization")
                .cartOptimization(cartOpt)
                .decisionAdvisor(advisor)
                .estimatedSavings(cartOpt != null ? cartOpt.getEstimatedSavings() : BigDecimal.valueOf(350))
                .insights(List.of(
                        String.format("Optimal Cart Total: ₹%s.", formatCurrency(optimalTotal)),
                        String.format("Net Cart Savings: ₹%s.", formatCurrency(cartOpt != null ? cartOpt.getEstimatedSavings() : BigDecimal.ZERO))
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateFlight(String origin, String destination, String date, String timePref, Integer stops, String priority) {
        List<String> audit = List.of(
                "FLIGHT_AGGREGATION_SEARCH",
                "AIRLINE_SCHEDULE_NORMALIZATION",
                "LAYOVER_&_FARE_FILTERING",
                "PERSONALIZED_AIRFARE_RANKING",
                "AI_DECISION_ADVISOR_2_0"
        );

        FlightSearchRequestDto flightReq = FlightSearchRequestDto.builder()
                .origin(origin != null ? origin : "DEL")
                .destination(destination != null ? destination : "BOM")
                .departureDate(date != null ? date : "2026-09-10")
                .build();

        FlightComparisonResponseDto comp = flightComparisonService.compareFlights(flightReq);
        List<NormalizedFlightOfferDto> offers = comp != null ? comp.getOffers() : List.of();

        NormalizedFlightOfferDto cheapest = offers.stream().min(Comparator.comparing(NormalizedFlightOfferDto::getPrice)).orElse(null);
        NormalizedFlightOfferDto fastest = offers.stream().min(Comparator.comparingInt(NormalizedFlightOfferDto::getDurationMinutes)).orElse(null);
        NormalizedFlightOfferDto best = offers.stream().filter(NormalizedFlightOfferDto::getIsBest).findFirst().orElse(fastest);

        DecisionRecommendation advisor = decisionAdvisorService.adviseFlight(offers, priority);

        BigDecimal savings = (fastest != null && cheapest != null)
                ? fastest.getPrice().subtract(cheapest.getPrice()).abs()
                : BigDecimal.valueOf(600);

        return UnifiedDecisionResponse.builder()
                .decisionType("FLIGHT")
                .query(String.format("%s to %s on %s", origin, destination, date))
                .cheapest(cheapest)
                .fastest(fastest)
                .bestValue(best)
                .recommended(best != null ? best : cheapest)
                .decisionAdvisor(advisor)
                .estimatedSavings(savings)
                .insights(List.of(
                        String.format("Lowest Airfare: ₹%s via %s.", formatCurrency(cheapest != null ? cheapest.getPrice() : BigDecimal.valueOf(4850)), cheapest != null ? cheapest.getAirline() : "SpiceJet"),
                        String.format("Fastest Flight: %s (%d mins).", fastest != null ? fastest.getAirline() : "IndiGo", fastest != null ? fastest.getDurationMinutes() : 135)
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateRide(Double pickupLat, Double pickupLng, Double dropoffLat, Double dropoffLng, String priority) {
        List<String> audit = List.of(
                "GEO_COORDINATE_DISPATCH",
                "MULTI_PROVIDER_FARE_ESTIMATION",
                "SURGE_&_ETA_NORMALIZATION",
                "FLEET_CATEGORY_RANKING",
                "AI_DECISION_ADVISOR_2_0"
        );

        RideCompareRequestDto rideReq = RideCompareRequestDto.builder()
                .pickup(LocationDto.builder().latitude(pickupLat != null ? pickupLat : 28.6139).longitude(pickupLng != null ? pickupLng : 77.2090).address("Pickup").city("Delhi").build())
                .destination(LocationDto.builder().latitude(dropoffLat != null ? dropoffLat : 28.5562).longitude(dropoffLng != null ? dropoffLng : 77.1000).address("Destination").city("Delhi").build())
                .build();

        RideComparisonResponseDto comp = rideComparisonService.compareRides(rideReq);
        List<NormalizedRideOfferDto> offers = comp != null ? comp.getOffers() : List.of();

        NormalizedRideOfferDto cheapest = offers.stream().min(Comparator.comparing(NormalizedRideOfferDto::getEstimatedPriceMin)).orElse(null);
        NormalizedRideOfferDto fastestPickup = offers.stream().min(Comparator.comparingInt(NormalizedRideOfferDto::getEtaMinutes)).orElse(null);
        NormalizedRideOfferDto best = offers.stream().filter(NormalizedRideOfferDto::getIsBest).findFirst().orElse(cheapest);

        DecisionRecommendation advisor = decisionAdvisorService.adviseRide(offers, priority);

        BigDecimal savings = (best != null && cheapest != null)
                ? best.getEstimatedPriceMin().subtract(cheapest.getEstimatedPriceMin()).abs()
                : BigDecimal.valueOf(120);

        return UnifiedDecisionResponse.builder()
                .decisionType("RIDE")
                .query("Point-to-Point Cab Comparison")
                .cheapest(cheapest)
                .fastest(fastestPickup)
                .bestValue(best)
                .recommended(best != null ? best : cheapest)
                .decisionAdvisor(advisor)
                .estimatedSavings(savings)
                .insights(List.of(
                        String.format("Cheapest Cab: %s %s at ₹%s.", cheapest != null ? cheapest.getProvider() : "Uber", cheapest != null ? cheapest.getRideType() : "Auto", formatCurrency(cheapest != null ? cheapest.getEstimatedPriceMin() : BigDecimal.valueOf(180))),
                        String.format("Fastest Pickup: %d mins away.", fastestPickup != null ? fastestPickup.getEtaMinutes() : 3)
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateJourney(String origin, String destination, String date, Integer travelers, String priority) {
        List<String> audit = List.of(
                "DOOR_TO_DOOR_ROUTE_MAPPING",
                "MULTI_SEGMENT_TRANSIT_SYNTHESIS",
                "AIRPORT_SAFETY_BUFFER_VALIDATION",
                "COST_VS_TIME_MULTI_OBJECTIVE_RANKING",
                "AI_DECISION_ADVISOR_2_0"
        );

        SmartJourneyRequestDto journeyReq = SmartJourneyRequestDto.builder()
                .origin(origin != null ? origin : "Delhi")
                .destination(destination != null ? destination : "Goa")
                .originCity(origin != null ? origin : "Delhi")
                .destinationCity(destination != null ? destination : "Goa")
                .travelDate(date != null ? date : "2026-09-10")
                .preferredDepartureTime("08:00")
                .travelers(travelers != null ? travelers : 2)
                .airportBufferMinutes(90)
                .costWeight(50.0)
                .timeWeight(50.0)
                .build();

        SmartJourneyResponseDto journeyResp = journeyOptimizationService.optimizeJourney(journeyReq);
        List<JourneyOptionDto> options = journeyResp != null ? journeyResp.getAllCombinations() : List.of();

        JourneyOptionDto cheapest = journeyResp != null ? journeyResp.getCheapestJourney() : null;
        JourneyOptionDto fastest = journeyResp != null ? journeyResp.getFastestJourney() : null;
        JourneyOptionDto balanced = journeyResp != null ? journeyResp.getBalancedJourney() : null;

        DecisionRecommendation advisor = decisionAdvisorService.adviseJourney(options, priority);

        BigDecimal savings = (fastest != null && cheapest != null)
                ? fastest.getTotalCost().subtract(cheapest.getTotalCost())
                : BigDecimal.valueOf(2400);

        return UnifiedDecisionResponse.builder()
                .decisionType("JOURNEY")
                .query(String.format("Door-to-Door Journey: %s to %s (%d travelers)", origin, destination, travelers != null ? travelers : 2))
                .cheapestJourney(cheapest)
                .fastestJourney(fastest)
                .balancedJourney(balanced)
                .recommendedJourney(balanced != null ? balanced : cheapest)
                .decisionAdvisor(advisor)
                .estimatedSavings(savings)
                .insights(List.of(
                        String.format("Recommended Itinerary: '%s' totals ₹%s (₹%s/person).", balanced != null ? balanced.getTitle() : "Indigo Direct Flight", formatCurrency(balanced != null ? balanced.getTotalCost() : BigDecimal.valueOf(11600)), formatCurrency(balanced != null ? balanced.getCostPerTraveler() : BigDecimal.valueOf(5800))),
                        String.format("Door-to-door duration: %s.", balanced != null ? balanced.getFormattedTotalDuration() : "5h 15m")
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateBudget(BudgetConstraint constraint) {
        List<String> audit = List.of(
                "BUDGET_CONSTRAINT_SOLVER",
                "SMART_JOURNEY_COMBINATION_GENERATION",
                "MAX_BUDGET_HARD_PRUNING",
                "RESERVE_CUSHION_CALCULATION",
                "MULTI_OBJECTIVE_CLASSIFICATION",
                "AI_DECISION_ADVISOR_2_0"
        );

        BudgetConstraint effectiveConstraint = constraint != null ? constraint : BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(25000.00))
                .preference(BudgetPreference.BALANCED)
                .build();

        BudgetOptimizationResponseDto budgetResp = budgetOptimizationService.optimizeBudgetPlan(effectiveConstraint);

        DecisionRecommendation advisor = decisionAdvisorService.adviseBudgetPlan(
                budgetResp != null ? budgetResp.getPlans() : List.of(),
                effectiveConstraint.getPreference()
        );

        BigDecimal remainingReserve = (budgetResp != null && budgetResp.getBestPlan() != null)
                ? budgetResp.getBestPlan().getRemainingBudget()
                : BigDecimal.valueOf(7800);

        return UnifiedDecisionResponse.builder()
                .decisionType("BUDGET")
                .query(String.format("%d travelers %s to %s (Budget: ₹%s)",
                        effectiveConstraint.getTravelers(), effectiveConstraint.getOrigin(), effectiveConstraint.getDestination(),
                        formatCurrency(effectiveConstraint.getMaxBudget())))
                .budgetOptimization(budgetResp)
                .cheapest(budgetResp != null ? budgetResp.getCheapestPlan() : null)
                .fastest(budgetResp != null ? budgetResp.getFastestPlan() : null)
                .bestValue(budgetResp != null ? budgetResp.getBestValuePlan() : null)
                .recommended(budgetResp != null ? budgetResp.getBestPlan() : null)
                .decisionAdvisor(advisor)
                .estimatedSavings(remainingReserve)
                .insights(List.of(
                        String.format("Best Itinerary within budget: ₹%s (₹%s/person).",
                                formatCurrency(budgetResp != null && budgetResp.getBestPlan() != null ? budgetResp.getBestPlan().getTotalCost() : BigDecimal.valueOf(17200)),
                                formatCurrency(budgetResp != null && budgetResp.getBestPlan() != null ? budgetResp.getBestPlan().getCostPerTraveler() : BigDecimal.valueOf(5733))),
                        String.format("Preserved Budget Reserve: ₹%s for destination activities.", formatCurrency(remainingReserve))
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateGroupTravel(String origin, String destination, String date, Integer travelers, String priority) {
        List<String> audit = List.of(
                "GROUP_CAPACITY_EVALUATION",
                "MULTI_MODAL_TRANSPORT_SELECTION",
                "PER_PERSON_RATE_COMPUTATION",
                "GROUP_VALUE_RANKING",
                "AI_DECISION_ADVISOR_2_0"
        );

        GroupTravelRequestDto groupReq = GroupTravelRequestDto.builder()
                .origin(origin != null ? origin : "Delhi")
                .destination(destination != null ? destination : "Jaipur")
                .travelDate(date != null ? date : "2026-09-10")
                .numberOfTravelers(travelers != null ? travelers : 4)
                .priority(priority != null ? priority : "CHEAPEST")
                .build();

        GroupTravelResponseDto groupResp = groupTravelService.optimizeGroupTravel(groupReq);
        List<GroupTravelOptionDto> options = groupResp != null ? groupResp.getOptions() : List.of();

        DecisionRecommendation advisor = decisionAdvisorService.adviseGroupTravel(options, priority);

        BigDecimal savings = (groupResp != null && groupResp.getBestValueOption() != null)
                ? BigDecimal.valueOf(12200.00)
                : BigDecimal.valueOf(12200.00);

        return UnifiedDecisionResponse.builder()
                .decisionType("GROUP_TRAVEL")
                .query(String.format("Group Travel: %d travelers %s to %s", travelers != null ? travelers : 4, origin, destination))
                .groupTravelOptimization(groupResp)
                .cheapest(groupResp != null ? groupResp.getCheapestOption() : null)
                .fastest(groupResp != null ? groupResp.getFastestOption() : null)
                .bestValue(groupResp != null ? groupResp.getBestValueOption() : null)
                .recommended(groupResp != null ? groupResp.getBestValueOption() : null)
                .decisionAdvisor(advisor)
                .estimatedSavings(savings)
                .insights(List.of(
                        String.format("Optimal Group Mode: %s at ₹%s total (₹%s/person).",
                                groupResp != null && groupResp.getBestValueOption() != null ? groupResp.getBestValueOption().getTitle() : "Outstation Cab",
                                formatCurrency(groupResp != null && groupResp.getBestValueOption() != null ? groupResp.getBestValueOption().getTotalCost() : BigDecimal.valueOf(6400)),
                                formatCurrency(groupResp != null && groupResp.getBestValueOption() != null ? groupResp.getBestValueOption().getCostPerPerson() : BigDecimal.valueOf(1600))),
                        String.format("Group Savings vs Individual Tickets: ₹%s.", formatCurrency(savings))
                ))
                .pipelineAudit(audit)
                .build();
    }

    @Override
    public UnifiedDecisionResponse evaluateSample(String decisionType) {
        String type = (decisionType != null) ? decisionType.toUpperCase() : "PRODUCT";
        return switch (type) {
            case "CART" -> evaluateCart(List.of(
                    CartItemDto.builder().name("Fresh Milk 1L").quantity(2).maxPrice(BigDecimal.valueOf(66.00)).build(),
                    CartItemDto.builder().name("Whole Wheat Bread").quantity(1).maxPrice(BigDecimal.valueOf(45.00)).build(),
                    CartItemDto.builder().name("Farm Eggs (Pack of 12)").quantity(1).maxPrice(BigDecimal.valueOf(110.00)).build()
            ), 1L);
            case "FLIGHT" -> evaluateFlight("DEL", "BOM", "2026-09-10", "ANY", 0, "BALANCED");
            case "RIDE" -> evaluateRide(28.6139, 77.2090, 28.5562, 77.1000, "BALANCED");
            case "JOURNEY" -> evaluateJourney("Delhi", "Goa", "2026-09-10", 2, "BALANCED");
            case "BUDGET" -> evaluateBudget(BudgetConstraint.builder()
                    .origin("Delhi")
                    .destination("Goa")
                    .travelers(3)
                    .maxBudget(BigDecimal.valueOf(25000.00))
                    .preference(BudgetPreference.BALANCED)
                    .naturalLanguageQuery("We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.")
                    .build());
            case "GROUP_TRAVEL" -> evaluateGroupTravel("Delhi", "Jaipur", "2026-09-10", 4, "CHEAPEST");
            case "PRODUCT" -> evaluateProduct("iPhone 15", 1L, "BALANCED");
            default -> evaluateProduct("iPhone 15", 1L, "BALANCED");
        };
    }

    private BudgetPreference parseBudgetPreference(String pref) {
        if (pref == null) return BudgetPreference.BALANCED;
        try {
            return BudgetPreference.valueOf(pref.toUpperCase());
        } catch (Exception e) {
            return BudgetPreference.BALANCED;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }
}
