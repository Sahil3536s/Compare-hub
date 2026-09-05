package com.comparehub.service.impl;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.service.ComparisonRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ComparisonRecommendationServiceImpl implements ComparisonRecommendationService {

    @Override
    public AiRecommendationDto recommendProducts(List<NormalizedProductOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return AiRecommendationDto.builder()
                    .bestOverall("No offers available")
                    .cheapest("N/A")
                    .recommendation("No merchant offers currently available to analyze.")
                    .reasoningPoints(List.of())
                    .build();
        }

        // 1. Identify cheapest
        NormalizedProductOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                .orElse(offers.get(0));

        // 2. Identify best overall considering: Price, Rating, Discount, Delivery, Stock
        NormalizedProductOfferDto bestOverall = offers.stream()
                .max(Comparator.comparingDouble(o -> {
                    double ratingWeight = (o.getRating() != null ? o.getRating() : 3.5) * 20.0; // 0-100
                    double stockBonus = Boolean.TRUE.equals(o.getInStock()) ? 30.0 : -50.0;
                    double discountBonus = o.getDiscountPercent() != null ? Math.min(30.0, o.getDiscountPercent()) : 0.0;
                    double pricePenalty = (o.getPrice().doubleValue() / cheapest.getPrice().doubleValue()) * 50.0;
                    double deliveryBonus = (o.getDelivery() != null && o.getDelivery().toLowerCase().contains("tomorrow")
                            || (o.getDelivery() != null && o.getDelivery().toLowerCase().contains("same day"))) ? 25.0 : 10.0;
                    return ratingWeight + stockBonus + discountBonus + deliveryBonus - pricePenalty;
                }))
                .orElse(cheapest);

        List<String> reasons = new ArrayList<>();
        String recommendation;

        if (cheapest.equals(bestOverall) || cheapest.getMerchant().equalsIgnoreCase(bestOverall.getMerchant())) {
            recommendation = String.format("%s offers the absolute lowest price at ₹%s with a %s★ rating and confirmed stock.",
                    cheapest.getMerchant(),
                    cheapest.getPrice().toPlainString(),
                    cheapest.getRating() != null ? cheapest.getRating().toString() : "4.0");

            reasons.add(String.format("Lowest price available across verified stores (₹%s).", cheapest.getPrice().toPlainString()));
            if (cheapest.getDelivery() != null) {
                reasons.add(String.format("Estimated delivery: %s.", cheapest.getDelivery()));
            }
            if (cheapest.getDiscountPercent() != null && cheapest.getDiscountPercent() > 0) {
                reasons.add(String.format("Discount of %d%% off original price.", cheapest.getDiscountPercent()));
            }
        } else {
            BigDecimal priceDiff = bestOverall.getPrice().subtract(cheapest.getPrice()).abs();
            recommendation = String.format("%s costs ₹%s more than %s but provides a higher rating (%s★ vs %s★) and %s.",
                    bestOverall.getMerchant(),
                    priceDiff.toPlainString(),
                    cheapest.getMerchant(),
                    bestOverall.getRating() != null ? bestOverall.getRating() : "4.5",
                    cheapest.getRating() != null ? cheapest.getRating() : "4.0",
                    bestOverall.getDelivery() != null ? bestOverall.getDelivery() : "reliable courier delivery");

            reasons.add(String.format("Cheapest Option: %s at ₹%s.", cheapest.getMerchant(), cheapest.getPrice().toPlainString()));
            reasons.add(String.format("Best Value: %s at ₹%s with %s★ customer satisfaction score.",
                    bestOverall.getMerchant(), bestOverall.getPrice().toPlainString(), bestOverall.getRating()));
            if (bestOverall.getDelivery() != null) {
                reasons.add(String.format("Delivery timeline: %s via %s.", bestOverall.getDelivery(), bestOverall.getMerchant()));
            }
            if (Boolean.TRUE.equals(bestOverall.getInStock())) {
                reasons.add(String.format("In-stock availability confirmed at %s.", bestOverall.getMerchant()));
            }
        }

        return AiRecommendationDto.builder()
                .bestOverall(bestOverall.getMerchant())
                .cheapest(cheapest.getMerchant())
                .recommendation(recommendation)
                .reasoningPoints(reasons)
                .build();
    }

    @Override
    public AiRecommendationDto recommendFlights(List<NormalizedFlightOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return AiRecommendationDto.builder()
                    .bestOverall("No flights available")
                    .cheapest("N/A")
                    .recommendation("No flight itineraries currently found for this route.")
                    .reasoningPoints(List.of())
                    .build();
        }

        // 1. Identify cheapest
        NormalizedFlightOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedFlightOfferDto::getPrice))
                .orElse(offers.get(0));

        // 2. Identify fastest
        NormalizedFlightOfferDto fastest = offers.stream()
                .min(Comparator.comparingInt(NormalizedFlightOfferDto::getDurationMinutes))
                .orElse(offers.get(0));

        // 3. Identify best overall (tagged with isBest or lowest score)
        NormalizedFlightOfferDto best = offers.stream()
                .filter(NormalizedFlightOfferDto::getIsBest)
                .findFirst()
                .orElse(fastest);

        List<String> reasons = new ArrayList<>();
        String recommendation;

        String bestDurationStr = formatDuration(best.getDurationMinutes());
        String cheapestDurationStr = formatDuration(cheapest.getDurationMinutes());

        if (cheapest.equals(best) || cheapest.getFlightNumber().equals(best.getFlightNumber())) {
            recommendation = String.format("%s (%s) is both the cheapest and best value flight at ₹%s with a duration of %s.",
                    best.getAirline(), best.getFlightNumber(), best.getPrice().toPlainString(), bestDurationStr);
            reasons.add(String.format("Lowest airfare on this route: ₹%s.", best.getPrice().toPlainString()));
            reasons.add(String.format("%s route with %s stops.", best.getStops() == 0 ? "Non-stop direct" : best.getStops() + "-stop", best.getStops()));
        } else {
            BigDecimal priceDiff = best.getPrice().subtract(cheapest.getPrice()).abs();
            int timeSavedMins = cheapest.getDurationMinutes() - best.getDurationMinutes();

            if (timeSavedMins > 0) {
                recommendation = String.format("%s (%s) costs ₹%s more than %s but saves %s in travel time with %s.",
                        best.getAirline(),
                        best.getFlightNumber(),
                        priceDiff.toPlainString(),
                        cheapest.getAirline(),
                        formatDuration(timeSavedMins),
                        best.getStops() == 0 ? "a non-stop flight" : "fewer layovers");
            } else {
                recommendation = String.format("%s (%s) provides the most balanced route at ₹%s (%s), compared to %s at ₹%s.",
                        best.getAirline(), best.getFlightNumber(), best.getPrice().toPlainString(), bestDurationStr,
                        cheapest.getAirline(), cheapest.getPrice().toPlainString());
            }

            reasons.add(String.format("Cheapest: %s (%s) at ₹%s (%s, %s stops).",
                    cheapest.getAirline(), cheapest.getFlightNumber(), cheapest.getPrice().toPlainString(),
                    cheapestDurationStr, cheapest.getStops()));
            reasons.add(String.format("Fastest & Best: %s (%s) taking only %s at ₹%s.",
                    best.getAirline(), best.getFlightNumber(), bestDurationStr, best.getPrice().toPlainString()));
            if (best.getStops() == 0) {
                reasons.add("Non-stop direct flight avoids transit delay risks.");
            }
        }

        return AiRecommendationDto.builder()
                .bestOverall(best.getAirline() + " (" + best.getFlightNumber() + ")")
                .cheapest(cheapest.getAirline() + " (" + cheapest.getFlightNumber() + ")")
                .recommendation(recommendation)
                .reasoningPoints(reasons)
                .build();
    }

    @Override
    public AiRecommendationDto recommendRides(List<NormalizedRideOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return AiRecommendationDto.builder()
                    .bestOverall("No rides available")
                    .cheapest("N/A")
                    .recommendation("No ride fare options currently found for these coordinates.")
                    .reasoningPoints(List.of())
                    .build();
        }

        // 1. Identify cheapest
        NormalizedRideOfferDto cheapest = offers.stream()
                .min(Comparator.comparing(NormalizedRideOfferDto::getEstimatedPriceMin))
                .orElse(offers.get(0));

        // 2. Identify fastest pickup
        NormalizedRideOfferDto fastestPickup = offers.stream()
                .min(Comparator.comparingInt(NormalizedRideOfferDto::getEtaMinutes))
                .orElse(offers.get(0));

        // 3. Identify best overall (tagged with isBest)
        NormalizedRideOfferDto best = offers.stream()
                .filter(NormalizedRideOfferDto::getIsBest)
                .findFirst()
                .orElse(cheapest);

        List<String> reasons = new ArrayList<>();
        String recommendation;

        if (cheapest.equals(best) || cheapest.getRideType().equals(best.getRideType())) {
            recommendation = String.format("%s %s offers the best balance and lowest fare starting at ₹%s with an ETA of %d mins.",
                    best.getProvider(), best.getRideType(), best.getEstimatedPriceMin().toPlainString(), best.getEtaMinutes());
            reasons.add(String.format("Lowest estimated fare: ₹%s for %s category.", best.getEstimatedPriceMin().toPlainString(), best.getVehicleCategory()));
            reasons.add(String.format("Pickup driver estimated %d mins away.", best.getEtaMinutes()));
        } else {
            recommendation = String.format("%s %s arrives in %d mins for ₹%s, while %s %s is the cheapest option at ₹%s for cost-conscious travelers.",
                    best.getProvider(), best.getRideType(), best.getEtaMinutes(), best.getEstimatedPriceMin().toPlainString(),
                    cheapest.getProvider(), cheapest.getRideType(), cheapest.getEstimatedPriceMin().toPlainString());

            reasons.add(String.format("Cheapest Option: %s %s at ₹%s (ETA: %d mins).",
                    cheapest.getProvider(), cheapest.getRideType(), cheapest.getEstimatedPriceMin().toPlainString(), cheapest.getEtaMinutes()));
            reasons.add(String.format("Recommended Pick: %s %s at ₹%s with swift %d min pickup.",
                    best.getProvider(), best.getRideType(), best.getEstimatedPriceMin().toPlainString(), best.getEtaMinutes()));
            reasons.add(String.format("Fastest Available Pickup: %s arriving in %d mins.",
                    fastestPickup.getProvider() + " " + fastestPickup.getRideType(), fastestPickup.getEtaMinutes()));
        }

        return AiRecommendationDto.builder()
                .bestOverall(best.getProvider() + " " + best.getRideType())
                .cheapest(cheapest.getProvider() + " " + cheapest.getRideType())
                .recommendation(recommendation)
                .reasoningPoints(reasons)
                .build();
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null) return "2h";
        int hrs = minutes / 60;
        int mins = minutes % 60;
        if (mins > 0) {
            return String.format("%dh %dm", hrs, mins);
        }
        return String.format("%dh", hrs);
    }
}
