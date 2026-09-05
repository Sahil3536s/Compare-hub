package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;
import com.comparehub.service.DecisionAdvisorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class DecisionAdvisorServiceImpl implements DecisionAdvisorService {

    @Override
    public DecisionRecommendation advise(DecisionContext context) {
        if (context == null) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No context provided")
                    .summary("Unable to analyze decision without comparison context.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("UNKNOWN")
                    .build();
        }

        String type = context.getContextType() != null ? context.getContextType().toUpperCase() : "PRODUCT";
        String priority = context.getUserPriority() != null ? context.getUserPriority() : "BALANCED";

        return switch (type) {
            case "PRODUCT" -> adviseProduct(context.getProductOffers(), priority);
            case "FLIGHT" -> adviseFlight(context.getFlightOffers(), priority);
            case "RIDE" -> adviseRide(context.getRideOffers(), priority);
            case "SMART_JOURNEY" -> adviseJourney(context.getJourneyOptions(), priority);
            case "GROUP_TRAVEL" -> adviseGroupTravel(context.getGroupTravelOptions(), priority);
            case "BUDGET_PLAN" -> {
                BudgetPreference pref = BudgetPreference.BALANCED;
                try {
                    pref = BudgetPreference.valueOf(priority.toUpperCase());
                } catch (Exception ignored) {
                }
                yield adviseBudgetPlan(context.getBudgetPlans(), pref);
            }
            default -> adviseProduct(context.getProductOffers(), priority);
        };
    }

    @Override
    public DecisionRecommendation adviseProduct(List<NormalizedProductOfferDto> offers, String userPriority) {
        if (offers == null || offers.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No offers available")
                    .summary("No merchant offers currently available to analyze.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("PRODUCT")
                    .build();
        }

        // 1. Identify cheapest
        NormalizedProductOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                .orElse(offers.get(0));

        // 2. Identify highest rated
        NormalizedProductOfferDto topRated = offers.stream()
                .max(Comparator.comparingDouble(o -> o.getRating() != null ? o.getRating() : 0.0))
                .orElse(cheapest);

        // 3. Identify best overall balance
        NormalizedProductOfferDto bestOverall = offers.stream()
                .max(Comparator.comparingDouble(o -> {
                    double ratingScore = (o.getRating() != null ? o.getRating() : 3.5) * 20.0;
                    double stockScore = Boolean.TRUE.equals(o.getInStock()) ? 30.0 : -50.0;
                    double discountScore = o.getDiscountPercent() != null ? Math.min(30.0, o.getDiscountPercent()) : 0.0;
                    double pricePenalty = (o.getPrice().doubleValue() / cheapest.getPrice().doubleValue()) * 50.0;
                    double deliveryScore = (o.getDelivery() != null && (o.getDelivery().toLowerCase().contains("tomorrow")
                            || o.getDelivery().toLowerCase().contains("same day") || o.getDelivery().toLowerCase().contains("prime"))) ? 25.0 : 10.0;
                    return ratingScore + stockScore + discountScore + deliveryScore - pricePenalty;
                }))
                .orElse(cheapest);

        // Select recommendation based on priority
        NormalizedProductOfferDto recommended = bestOverall;
        if ("CHEAPEST".equalsIgnoreCase(userPriority) || "PRICE".equalsIgnoreCase(userPriority)) {
            recommended = cheapest;
        } else if ("RATING".equalsIgnoreCase(userPriority)) {
            recommended = topRated;
        }

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();
        String summary;
        String confidence = offers.size() >= 3 ? "HIGH" : (offers.size() == 2 ? "MEDIUM" : "LOW");

        if (cheapest.equals(bestOverall) || cheapest.getMerchant().equalsIgnoreCase(bestOverall.getMerchant())) {
            summary = String.format("%s provides both the lowest price at ₹%s and top overall value with %s★ seller rating and confirmed stock.",
                    cheapest.getMerchant(),
                    formatCurrency(cheapest.getPrice()),
                    cheapest.getRating() != null ? cheapest.getRating().toString() : "4.0");

            reasons.add(String.format("Lowest price across verified stores: ₹%s.", formatCurrency(cheapest.getPrice())));
            if (cheapest.getDelivery() != null) {
                reasons.add(String.format("Fulfillment timeline: %s via %s.", cheapest.getDelivery(), cheapest.getMerchant()));
            }
            if (cheapest.getDiscountPercent() != null && cheapest.getDiscountPercent() > 0) {
                reasons.add(String.format("Verified discount: %d%% off list price.", cheapest.getDiscountPercent()));
            }
            tradeoffs.add("No significant compromise needed; pricing and seller rating are aligned as the market leader.");
        } else {
            BigDecimal priceDiff = bestOverall.getPrice().subtract(cheapest.getPrice()).abs();
            double ratingDiff = (bestOverall.getRating() != null ? bestOverall.getRating() : 4.0) -
                    (cheapest.getRating() != null ? cheapest.getRating() : 3.5);

            String deliveryA = bestOverall.getDelivery() != null ? bestOverall.getDelivery() : "standard shipping";
            String deliveryB = cheapest.getDelivery() != null ? cheapest.getDelivery() : "economy shipping";

            summary = String.format("%s costs ₹%s more than %s, but has faster delivery (%s) and a higher seller rating (%.1f★ vs %.1f★). If price is your priority choose %s; otherwise %s offers better overall value.",
                    bestOverall.getMerchant(),
                    formatCurrency(priceDiff),
                    cheapest.getMerchant(),
                    deliveryA,
                    bestOverall.getRating() != null ? bestOverall.getRating() : 4.5,
                    cheapest.getRating() != null ? cheapest.getRating() : 4.0,
                    cheapest.getMerchant(),
                    bestOverall.getMerchant());

            reasons.add(String.format("%s offers higher seller satisfaction score (%.1f★ vs %.1f★).",
                    bestOverall.getMerchant(),
                    bestOverall.getRating() != null ? bestOverall.getRating() : 4.5,
                    cheapest.getRating() != null ? cheapest.getRating() : 4.0));
            reasons.add(String.format("Delivery speed: %s with %s vs %s with %s.",
                    bestOverall.getMerchant(), deliveryA, cheapest.getMerchant(), deliveryB));
            if (Boolean.TRUE.equals(bestOverall.getInStock())) {
                reasons.add(String.format("Immediate dispatch confirmed in-stock at %s.", bestOverall.getMerchant()));
            }

            tradeoffs.add(String.format("Paying ₹%s extra on %s for faster delivery and superior merchant rating.",
                    formatCurrency(priceDiff), bestOverall.getMerchant()));
            tradeoffs.add(String.format("Opting for %s saves ₹%s but may require longer delivery transit (%s).",
                    cheapest.getMerchant(), formatCurrency(priceDiff), deliveryB));
        }

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s (₹%s)", recommended.getMerchant(), formatCurrency(recommended.getPrice())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence(confidence)
                .deterministic(true)
                .contextType("PRODUCT")
                .build();
    }

    @Override
    public DecisionRecommendation adviseFlight(List<NormalizedFlightOfferDto> offers, String userPriority) {
        if (offers == null || offers.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No flights available")
                    .summary("No flight itineraries currently found for this route.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("FLIGHT")
                    .build();
        }

        NormalizedFlightOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedFlightOfferDto::getPrice))
                .orElse(offers.get(0));

        NormalizedFlightOfferDto fastest = offers.stream()
                .min(Comparator.comparingInt(NormalizedFlightOfferDto::getDurationMinutes))
                .orElse(offers.get(0));

        NormalizedFlightOfferDto best = offers.stream()
                .filter(NormalizedFlightOfferDto::getIsBest)
                .findFirst()
                .orElse(fastest);

        NormalizedFlightOfferDto recommended = best;
        if ("CHEAPEST".equalsIgnoreCase(userPriority) || "PRICE".equalsIgnoreCase(userPriority)) {
            recommended = cheapest;
        } else if ("FASTEST".equalsIgnoreCase(userPriority) || "TIME".equalsIgnoreCase(userPriority)) {
            recommended = fastest;
        }

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();
        String summary;
        String confidence = offers.size() >= 3 ? "HIGH" : "MEDIUM";

        if (cheapest.equals(best) || cheapest.getFlightNumber().equals(best.getFlightNumber())) {
            summary = String.format("%s (%s) is both the lowest fare at ₹%s and the most efficient itinerary with a travel time of %s.",
                    best.getAirline(), best.getFlightNumber(), formatCurrency(best.getPrice()), formatDuration(best.getDurationMinutes()));

            reasons.add(String.format("Lowest airfare on this route: ₹%s.", formatCurrency(best.getPrice())));
            reasons.add(String.format("%s route with %d stops.", best.getStops() == 0 ? "Non-stop direct" : (best.getStops() + "-stop"), best.getStops()));
            tradeoffs.add("Zero trade-off required; lowest price matches fastest direct route.");
        } else {
            BigDecimal priceDiff = best.getPrice().subtract(cheapest.getPrice()).abs();
            int timeSavedMins = cheapest.getDurationMinutes() - best.getDurationMinutes();
            int stopDiff = cheapest.getStops() - best.getStops();

            if (timeSavedMins > 0) {
                summary = String.format("%s (%s) costs ₹%s more but saves %s in travel time and requires %s. If time is critical choose %s; if budget is constrained choose %s.",
                        best.getAirline(),
                        best.getFlightNumber(),
                        formatCurrency(priceDiff),
                        formatDuration(timeSavedMins),
                        stopDiff > 0 ? (stopDiff + " fewer transfer" + (stopDiff > 1 ? "s" : "")) : "fewer delays",
                        best.getAirline(),
                        cheapest.getAirline());
            } else {
                summary = String.format("%s (%s) offers the most balanced flight schedule at ₹%s (%s), compared to %s at ₹%s.",
                        best.getAirline(), best.getFlightNumber(), formatCurrency(best.getPrice()), formatDuration(best.getDurationMinutes()),
                        cheapest.getAirline(), formatCurrency(cheapest.getPrice()));
            }

            reasons.add(String.format("Fastest & Best: %s (%s) takes %s at ₹%s.",
                    best.getAirline(), best.getFlightNumber(), formatDuration(best.getDurationMinutes()), formatCurrency(best.getPrice())));
            reasons.add(String.format("Cheapest Alternative: %s (%s) at ₹%s (%s, %d stop%s).",
                    cheapest.getAirline(), cheapest.getFlightNumber(), formatCurrency(cheapest.getPrice()),
                    formatDuration(cheapest.getDurationMinutes()), cheapest.getStops(), cheapest.getStops() == 1 ? "" : "s"));
            if (best.getStops() == 0) {
                reasons.add("Non-stop direct flight eliminates layover baggage transfer risks.");
            }

            tradeoffs.add(String.format("Paying ₹%s fare premium on %s saves %s of journey duration.",
                    formatCurrency(priceDiff), best.getAirline(), formatDuration(timeSavedMins > 0 ? timeSavedMins : 60)));
            tradeoffs.add(String.format("Saving ₹%s on %s adds layover time and potential flight connection overhead.",
                    formatCurrency(priceDiff), cheapest.getAirline()));
        }

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s %s (₹%s)", recommended.getAirline(), recommended.getFlightNumber(), formatCurrency(recommended.getPrice())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence(confidence)
                .deterministic(true)
                .contextType("FLIGHT")
                .build();
    }

    @Override
    public DecisionRecommendation adviseRide(List<NormalizedRideOfferDto> offers, String userPriority) {
        if (offers == null || offers.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No rides available")
                    .summary("No ride fare options currently found for this route.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("RIDE")
                    .build();
        }

        NormalizedRideOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedRideOfferDto::getEstimatedPriceMin))
                .orElse(offers.get(0));

        NormalizedRideOfferDto fastestPickup = offers.stream()
                .min(Comparator.comparingInt(NormalizedRideOfferDto::getEtaMinutes))
                .orElse(offers.get(0));

        NormalizedRideOfferDto best = offers.stream()
                .filter(NormalizedRideOfferDto::getIsBest)
                .findFirst()
                .orElse(cheapest);

        NormalizedRideOfferDto recommended = best;
        if ("CHEAPEST".equalsIgnoreCase(userPriority)) {
            recommended = cheapest;
        } else if ("FASTEST".equalsIgnoreCase(userPriority)) {
            recommended = fastestPickup;
        }

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();
        String summary;

        if (cheapest.equals(best) || cheapest.getRideType().equals(best.getRideType())) {
            summary = String.format("%s %s offers the best combination of lowest fare at ₹%s and fast pickup in %d mins.",
                    best.getProvider(), best.getRideType(), formatCurrency(best.getEstimatedPriceMin()), best.getEtaMinutes());
            reasons.add(String.format("Lowest estimated fare: ₹%s (%s).", formatCurrency(best.getEstimatedPriceMin()), best.getVehicleCategory()));
            reasons.add(String.format("Nearby driver with %d min arrival ETA.", best.getEtaMinutes()));
            tradeoffs.add("Optimal price and availability with no trade-off required.");
        } else {
            BigDecimal priceDiff = best.getEstimatedPriceMin().subtract(cheapest.getEstimatedPriceMin()).abs();
            int etaDiff = cheapest.getEtaMinutes() - best.getEtaMinutes();

            summary = String.format("%s %s arrives in %d mins for ₹%s, while %s %s is ₹%s cheaper but has a %d min wait time.",
                    best.getProvider(), best.getRideType(), best.getEtaMinutes(), formatCurrency(best.getEstimatedPriceMin()),
                    cheapest.getProvider(), cheapest.getRideType(), formatCurrency(priceDiff), cheapest.getEtaMinutes());

            reasons.add(String.format("Recommended Pick: %s %s at ₹%s with %d min pickup.",
                    best.getProvider(), best.getRideType(), formatCurrency(best.getEstimatedPriceMin()), best.getEtaMinutes()));
            reasons.add(String.format("Cheapest Option: %s %s starting at ₹%s.",
                    cheapest.getProvider(), cheapest.getRideType(), formatCurrency(cheapest.getEstimatedPriceMin())));
            if (fastestPickup.getEtaMinutes() < best.getEtaMinutes()) {
                reasons.add(String.format("Fastest Cab: %s arriving in %d mins.",
                        fastestPickup.getProvider() + " " + fastestPickup.getRideType(), fastestPickup.getEtaMinutes()));
            }

            tradeoffs.add(String.format("Paying ₹%s extra gets you on the road %d minutes sooner.",
                    formatCurrency(priceDiff), Math.max(1, etaDiff)));
            tradeoffs.add(String.format("Choosing %s saves ₹%s at the expense of a %d-minute driver arrival buffer.",
                    cheapest.getRideType(), formatCurrency(priceDiff), cheapest.getEtaMinutes()));
        }

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s %s (₹%s)", recommended.getProvider(), recommended.getRideType(), formatCurrency(recommended.getEstimatedPriceMin())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence("HIGH")
                .deterministic(true)
                .contextType("RIDE")
                .build();
    }

    @Override
    public DecisionRecommendation adviseJourney(List<JourneyOptionDto> options, String userPriority) {
        if (options == null || options.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No journey combinations available")
                    .summary("No valid door-to-door transit itineraries found.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("SMART_JOURNEY")
                    .build();
        }

        JourneyOptionDto cheapest = options.stream()
                .min(Comparator.comparing(JourneyOptionDto::getTotalCost))
                .orElse(options.get(0));

        JourneyOptionDto fastest = options.stream()
                .min(Comparator.comparingInt(JourneyOptionDto::getTotalDurationMinutes))
                .orElse(options.get(0));

        JourneyOptionDto balanced = options.stream()
                .filter(JourneyOptionDto::isBalanced)
                .findFirst()
                .orElse(options.get(0));

        JourneyOptionDto recommended = balanced;
        if ("CHEAPEST".equalsIgnoreCase(userPriority)) {
            recommended = cheapest;
        } else if ("FASTEST".equalsIgnoreCase(userPriority)) {
            recommended = fastest;
        }

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();

        BigDecimal costDiff = fastest.getTotalCost().subtract(cheapest.getTotalCost()).abs();
        int timeSavedMins = cheapest.getTotalDurationMinutes() - fastest.getTotalDurationMinutes();

        String summary = String.format("Option '%s' (%s) provides the most optimal balance at ₹%s total (₹%s/person). Option '%s' is the fastest saving %s, while Option '%s' is the most budget-friendly at ₹%s.",
                balanced.getTitle(), balanced.getFormattedTotalDuration(), formatCurrency(balanced.getTotalCost()), formatCurrency(balanced.getCostPerTraveler()),
                fastest.getTitle(), formatDuration(timeSavedMins),
                cheapest.getTitle(), formatCurrency(cheapest.getTotalCost()));

        reasons.add(String.format("Balanced Pick: '%s' totals ₹%s for %s door-to-door transit.",
                balanced.getTitle(), formatCurrency(balanced.getTotalCost()), balanced.getFormattedTotalDuration()));
        reasons.add(String.format("Fastest Transit: '%s' completing journey in %s (%d transfer%s).",
                fastest.getTitle(), fastest.getFormattedTotalDuration(), fastest.getTransferCount(), fastest.getTransferCount() == 1 ? "" : "s"));
        reasons.add(String.format("Cheapest Transit: '%s' saving maximum cash at ₹%s total.",
                cheapest.getTitle(), formatCurrency(cheapest.getTotalCost())));

        tradeoffs.add(String.format("Fastest journey costs ₹%s more than cheapest but cuts transit time by %s.",
                formatCurrency(costDiff), formatDuration(timeSavedMins)));
        tradeoffs.add("Intercity road cabs avoid airport terminal queues but are subject to highway traffic fluctuations.");

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s (₹%s)", recommended.getTitle(), formatCurrency(recommended.getTotalCost())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence("HIGH")
                .deterministic(true)
                .contextType("SMART_JOURNEY")
                .build();
    }

    @Override
    public DecisionRecommendation adviseGroupTravel(List<GroupTravelOptionDto> options, String userPriority) {
        if (options == null || options.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No group travel options available")
                    .summary("No group travel configurations available.")
                    .reasons(List.of())
                    .tradeoffs(List.of())
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("GROUP_TRAVEL")
                    .build();
        }

        GroupTravelOptionDto cheapest = options.stream()
                .min(Comparator.comparing(GroupTravelOptionDto::getTotalCost))
                .orElse(options.get(0));

        GroupTravelOptionDto fastest = options.stream()
                .min(Comparator.comparingInt(GroupTravelOptionDto::getEstimatedTravelTimeMinutes))
                .orElse(options.get(0));

        GroupTravelOptionDto best = options.stream()
                .filter(GroupTravelOptionDto::isRecommended)
                .findFirst()
                .orElse(cheapest);

        GroupTravelOptionDto recommended = best;
        if ("CHEAPEST".equalsIgnoreCase(userPriority)) recommended = cheapest;
        else if ("FASTEST".equalsIgnoreCase(userPriority)) recommended = fastest;

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();

        String summary = String.format("For a group of %d, '%s' provides the best value at ₹%s (₹%s/person). Shared group transit delivers substantial per-traveler savings compared to purchasing %d individual flight tickets.",
                best.getNumberOfTravelers(), best.getTitle(), formatCurrency(best.getTotalCost()), formatCurrency(best.getCostPerPerson()), best.getNumberOfTravelers());

        reasons.add(String.format("Group per-person rate: ₹%s on '%s'.", formatCurrency(best.getCostPerPerson()), best.getTitle()));
        reasons.add(String.format("Vehicle capacity: Single shared %s fits all %d travelers comfortably.", best.getMode() != null ? best.getMode().name() : "vehicle", best.getNumberOfTravelers()));

        tradeoffs.add(String.format("Choosing group cab over flights saves ₹%s per person but extends journey time by %s.",
                formatCurrency(best.getCostPerPerson()), formatDuration(Math.abs(fastest.getEstimatedTravelTimeMinutes() - best.getEstimatedTravelTimeMinutes()))));

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s (₹%s total, ₹%s/person)", recommended.getTitle(), formatCurrency(recommended.getTotalCost()), formatCurrency(recommended.getCostPerPerson())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence("HIGH")
                .deterministic(true)
                .contextType("GROUP_TRAVEL")
                .build();
    }

    @Override
    public DecisionRecommendation adviseBudgetPlan(List<BudgetPlanOptionDto> plans, BudgetPreference preference) {
        if (plans == null || plans.isEmpty()) {
            return DecisionRecommendation.builder()
                    .recommendedOption("No plans within budget")
                    .summary("No door-to-door transit itineraries found within the specified maximum budget limit.")
                    .reasons(List.of("All available combinations exceed maximum allocated budget."))
                    .tradeoffs(List.of("Consider increasing the budget or adjusting travel dates."))
                    .confidence("LOW")
                    .deterministic(true)
                    .contextType("BUDGET_PLAN")
                    .build();
        }

        BudgetPlanOptionDto cheapest = plans.stream()
                .min(Comparator.comparing(BudgetPlanOptionDto::getTotalCost))
                .orElse(plans.get(0));

        BudgetPlanOptionDto fastest = plans.stream()
                .min(Comparator.comparingInt(BudgetPlanOptionDto::getDurationMinutes))
                .orElse(plans.get(0));

        BudgetPlanOptionDto best = switch (preference != null ? preference : BudgetPreference.BALANCED) {
            case CHEAPEST -> cheapest;
            case FASTEST -> fastest;
            case COMFORT -> plans.stream().filter(p -> "HIGH".equals(p.getComfortLevel())).findFirst().orElse(fastest);
            case BALANCED -> plans.stream().filter(p -> p.getJourneyOption() != null && p.getJourneyOption().isBalanced()).findFirst().orElse(plans.get(0));
        };

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();

        String summary = String.format("Recommended Plan: '%s' totals ₹%s (₹%s/person), leaving ₹%s (%.1f%%) in budget reserve.",
                best.getTitle(), formatCurrency(best.getTotalCost()), formatCurrency(best.getCostPerTraveler()),
                formatCurrency(best.getRemainingBudget()), (100.0 - best.getBudgetUtilizationPercent()));

        reasons.add(String.format("Strict Budget Compliance: Itinerary costs ₹%s against allocated budget.", formatCurrency(best.getTotalCost())));
        reasons.add(String.format("Remaining Reserve Cushion: ₹%s preserved for destination expenses.", formatCurrency(best.getRemainingBudget())));
        reasons.add(String.format("Door-to-door travel time: %s with comfort rating %s.", best.getFormattedDuration(), best.getComfortLevel()));

        tradeoffs.add(String.format("Option '%s' uses %s%% of budget, leaving ₹%s in reserve.",
                best.getTitle(), best.getBudgetUtilizationPercent(), formatCurrency(best.getRemainingBudget())));
        if (!cheapest.getId().equals(fastest.getId())) {
            tradeoffs.add(String.format("Fastest plan is %s faster than cheapest but utilizes ₹%s more budget.",
                    formatDuration(cheapest.getDurationMinutes() - fastest.getDurationMinutes()),
                    formatCurrency(fastest.getTotalCost().subtract(cheapest.getTotalCost()))));
        }

        return DecisionRecommendation.builder()
                .recommendedOption(String.format("%s (₹%s)", best.getTitle(), formatCurrency(best.getTotalCost())))
                .summary(summary)
                .reasons(reasons)
                .tradeoffs(tradeoffs)
                .confidence("HIGH")
                .deterministic(true)
                .contextType("BUDGET_PLAN")
                .build();
    }

    @Override
    public DecisionRecommendation getSampleProductAdvise() {
        return adviseProduct(List.of(
                NormalizedProductOfferDto.builder()
                        .merchant("Amazon")
                        .price(BigDecimal.valueOf(64999.00))
                        .rating(4.6)
                        .inStock(true)
                        .delivery("Tomorrow by 11 AM")
                        .discountPercent(12)
                        .build(),
                NormalizedProductOfferDto.builder()
                        .merchant("Croma")
                        .price(BigDecimal.valueOf(64299.00))
                        .rating(4.1)
                        .inStock(true)
                        .delivery("In 3-5 days")
                        .discountPercent(13)
                        .build(),
                NormalizedProductOfferDto.builder()
                        .merchant("Flipkart")
                        .price(BigDecimal.valueOf(65499.00))
                        .rating(4.4)
                        .inStock(true)
                        .delivery("In 2 days")
                        .discountPercent(11)
                        .build()
        ), "BALANCED");
    }

    @Override
    public DecisionRecommendation getSampleTravelAdvise() {
        return adviseFlight(List.of(
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
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .airline("Air India")
                        .flightNumber("AI-605")
                        .price(BigDecimal.valueOf(6200.00))
                        .durationMinutes(130)
                        .stops(0)
                        .isBest(false)
                        .build()
        ), "BALANCED");
    }

    private String formatDuration(int minutes) {
        int hrs = minutes / 60;
        int mins = Math.abs(minutes % 60);
        if (hrs > 0 && mins > 0) return String.format("%dh %dm", hrs, mins);
        if (hrs > 0) return String.format("%dh", hrs);
        return String.format("%dm", mins);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }
}
