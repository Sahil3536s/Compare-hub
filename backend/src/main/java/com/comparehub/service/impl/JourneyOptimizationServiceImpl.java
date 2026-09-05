package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.JourneySegmentType;
import com.comparehub.service.CostTimeOptimizationService;
import com.comparehub.service.JourneyOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyOptimizationServiceImpl implements JourneyOptimizationService {

    private final CostTimeOptimizationService costTimeOptimizationService;

    @Override
    public SmartJourneyResponseDto optimizeJourney(SmartJourneyRequestDto request) {
        long startTime = System.currentTimeMillis();

        String origin = (request.getOrigin() != null && !request.getOrigin().isBlank())
                ? request.getOrigin().trim()
                : "Saket, South Delhi";
        String destination = (request.getDestination() != null && !request.getDestination().isBlank())
                ? request.getDestination().trim()
                : "Calangute, North Goa";

        int travelers = Math.max(1, request.getTravelers());
        int bufferMinutes = Math.max(45, request.getAirportBufferMinutes() > 0 ? request.getAirportBufferMinutes() : 90);

        double costWeight = request.getCostWeight() >= 0 ? request.getCostWeight() : 50.0;
        double timeWeight = request.getTimeWeight() >= 0 ? request.getTimeWeight() : 50.0;

        if (costWeight + timeWeight > 0) {
            double total = costWeight + timeWeight;
            costWeight = (costWeight / total) * 100.0;
            timeWeight = 100.0 - costWeight;
        } else {
            costWeight = 50.0;
            timeWeight = 50.0;
        }

        List<JourneyOptionDto> combinations = generateFeasibleCombinations(origin, destination, travelers, bufferMinutes);

        // Convert to CostTimeOptionDto list for standard optimization
        List<CostTimeOptionDto> costTimeOptions = new ArrayList<>();
        Map<String, JourneyOptionDto> optionMap = new HashMap<>();

        for (JourneyOptionDto opt : combinations) {
            costTimeOptions.add(CostTimeOptionDto.builder()
                    .id(opt.getId())
                    .title(opt.getTitle())
                    .category("SMART_JOURNEY")
                    .cost(opt.getTotalCost())
                    .durationMinutes(opt.getTotalDurationMinutes())
                    .build());
            optionMap.put(opt.getId(), opt);
        }

        List<CostTimeOptionDto> ranked = costTimeOptimizationService.rankOptions(costTimeOptions, costWeight);

        JourneyOptionDto cheapest = combinations.stream()
                .min(Comparator.comparing(JourneyOptionDto::getTotalCost))
                .orElse(null);

        JourneyOptionDto fastest = combinations.stream()
                .min(Comparator.comparingInt(JourneyOptionDto::getTotalDurationMinutes))
                .orElse(null);

        List<CostTimeOptionDto> balancedRanked = costTimeOptimizationService.rankOptions(costTimeOptions, 50.0);
        JourneyOptionDto balanced = balancedRanked.isEmpty() ? null : optionMap.get(balancedRanked.get(0).getId());

        List<JourneyOptionDto> finalCombinations = new ArrayList<>();
        JourneyOptionDto top = null;

        for (int i = 0; i < ranked.size(); i++) {
            CostTimeOptionDto r = ranked.get(i);
            JourneyOptionDto opt = optionMap.get(r.getId());
            if (opt != null) {
                opt.setCostScore(r.getCostScore());
                opt.setTimeScore(r.getTimeScore());
                opt.setCompositeScore(r.getCompositeScore());
                opt.setClassification(r.getClassification());
                opt.setRecommendationReason(r.getTradeoffExplanation());

                if (cheapest != null && opt.getId().equals(cheapest.getId())) opt.setCheapest(true);
                if (fastest != null && opt.getId().equals(fastest.getId())) opt.setFastest(true);
                if (balanced != null && opt.getId().equals(balanced.getId())) opt.setBalanced(true);

                if (i == 0) {
                    top = opt;
                }
                finalCombinations.add(opt);
            }
        }

        CostTimeOptionDto cheapCt = cheapest != null ? CostTimeOptionDto.builder()
                .id(cheapest.getId()).title(cheapest.getTitle()).cost(cheapest.getTotalCost())
                .durationMinutes(cheapest.getTotalDurationMinutes())
                .formattedDuration(cheapest.getFormattedTotalDuration()).build() : null;

        CostTimeOptionDto fastCt = fastest != null ? CostTimeOptionDto.builder()
                .id(fastest.getId()).title(fastest.getTitle()).cost(fastest.getTotalCost())
                .durationMinutes(fastest.getTotalDurationMinutes())
                .formattedDuration(fastest.getFormattedTotalDuration()).build() : null;

        CostTimeOptionDto topCt = top != null ? CostTimeOptionDto.builder()
                .id(top.getId()).title(top.getTitle()).cost(top.getTotalCost())
                .durationMinutes(top.getTotalDurationMinutes())
                .formattedDuration(top.getFormattedTotalDuration()).build() : null;

        String tradeoffSummary = costTimeOptimizationService.generateTradeoffInsight(cheapCt, fastCt, topCt, costWeight);

        long execTime = System.currentTimeMillis() - startTime;

        return SmartJourneyResponseDto.builder()
                .origin(origin)
                .destination(destination)
                .travelers(travelers)
                .airportBufferMinutes(bufferMinutes)
                .costWeight(Math.round(costWeight * 10.0) / 10.0)
                .timeWeight(Math.round(timeWeight * 10.0) / 10.0)
                .tradeoffSummary(tradeoffSummary)
                .cheapestJourney(cheapest)
                .fastestJourney(fastest)
                .balancedJourney(balanced)
                .topRecommendedJourney(top)
                .allCombinations(finalCombinations)
                .executionTimeMs(execTime)
                .build();
    }

    @Override
    public SmartJourneyResponseDto getSampleJourney(String origin, String destination, int travelers, int bufferMinutes) {
        return optimizeJourney(SmartJourneyRequestDto.builder()
                .origin(origin)
                .destination(destination)
                .travelers(travelers)
                .airportBufferMinutes(bufferMinutes)
                .costWeight(50.0)
                .timeWeight(50.0)
                .build());
    }

    private List<JourneyOptionDto> generateFeasibleCombinations(String origin, String destination, int travelers, int bufferMinutes) {
        List<JourneyOptionDto> options = new ArrayList<>();

        String originCity = extractCity(origin, "Delhi");
        String destCity = extractCity(destination, "Goa");

        String originAirport = originCity.contains("Delhi") ? "Indira Gandhi Int'l Airport (DEL)" : originCity + " Airport";
        String destAirport = destCity.contains("Goa") ? "Goa Dabolim Airport (GOI)" : destCity + " Airport";

        // Combination 1: Balanced Smart Choice (Uber + IndiGo Non-stop + Ola Drop)
        options.add(buildOption(
                "opt-balanced-1",
                "IndiGo Express + Uber Connect",
                origin, originAirport, destAirport, destination,
                "06:00 AM", "06:40 AM", BigDecimal.valueOf(350.00), 40, "Uber Go", "🚗",
                bufferMinutes,
                "08:15 AM", "10:30 AM", BigDecimal.valueOf(4850.00), 135, "IndiGo 6E-204", "✈️",
                "11:00 AM", "11:50 AM", BigDecimal.valueOf(620.00), 50, "Ola Prime Sedan", "🚕",
                travelers,
                List.of(
                        "Synchronized with a " + bufferMinutes + "-minute security buffer at " + originAirport + ".",
                        "Non-stop flight avoids layover risk and minimizes total door-to-door transit time.",
                        "Total cost per person: ₹" + calculatePerPerson(BigDecimal.valueOf(350), BigDecimal.valueOf(4850), BigDecimal.valueOf(620), travelers)
                )
        ));

        // Combination 2: Budget Super Saver (Airport Shuttle + Akasa Air + Uber Go)
        options.add(buildOption(
                "opt-cheapest-2",
                "Akasa Air Value Saver + Airport Shuttle",
                origin, originAirport, destAirport, destination,
                "04:30 AM", "05:25 AM", BigDecimal.valueOf(140.00 * travelers), 55, "Airport Express Shuttle", "🚌",
                bufferMinutes,
                "07:00 AM", "09:25 AM", BigDecimal.valueOf(3900.00), 145, "Akasa Air QP-1322", "✈️",
                "09:55 AM", "10:45 AM", BigDecimal.valueOf(550.00), 50, "Uber Go", "🚗",
                travelers,
                List.of(
                        "Cheapest door-to-door flight itinerary across all surveyed airlines.",
                        "Early morning shuttle and flight saves ₹" + (4850 - 3900) + " per seat on airfare alone."
                )
        ));

        // Combination 3: Fastest Business Express (Ola Prime + Air India Direct + Airport Fast-Cab)
        options.add(buildOption(
                "opt-fastest-3",
                "Air India Prime Express + Priority Drop",
                origin, originAirport, destAirport, destination,
                "09:00 AM", "09:30 AM", BigDecimal.valueOf(520.00), 30, "Ola Prime Priority", "🚗",
                bufferMinutes,
                "11:05 AM", "13:10 PM", BigDecimal.valueOf(5900.00), 125, "Air India AI-804", "✈️",
                "13:40 PM", "14:20 PM", BigDecimal.valueOf(780.00), 40, "BluSmart EV Express", "🚕",
                travelers,
                List.of(
                        "Fastest door-to-door transit time with expedited highway priority cabs.",
                        "Includes complimentary hot meal and flexible cancellation."
                )
        ));

        // Combination 4: Direct Intercity XL Cab (For group or road travel feasibility comparison)
        if (travelers >= 2 || originCity.equalsIgnoreCase("Delhi") && destCity.equalsIgnoreCase("Jaipur")) {
            int cabDuration = 310; // ~5h 10m
            BigDecimal cabPrice = BigDecimal.valueOf(6400.00);

            JourneySegmentDto cabSegment = JourneySegmentDto.builder()
                    .id("seg-cab-direct")
                    .type(JourneySegmentType.DIRECT_INTERCITY_CAB)
                    .typeName("Direct Intercity Cab")
                    .provider("Uber XL Intercity")
                    .origin(origin)
                    .destination(destination)
                    .departureTime("07:00 AM")
                    .arrivalTime("12:10 PM")
                    .price(cabPrice)
                    .durationMinutes(cabDuration)
                    .formattedDuration(formatDuration(cabDuration))
                    .icon("🚐")
                    .metadata(Map.of("vehicleType", "Toyota Innova Crysta / Ertiga", "tollIncluded", "Yes", "doorToDoor", "Direct"))
                    .build();

            options.add(JourneyOptionDto.builder()
                    .id("opt-intercity-cab")
                    .title("Direct Uber XL Intercity (0 Transfers)")
                    .segments(List.of(cabSegment))
                    .totalCost(cabPrice)
                    .totalDurationMinutes(cabDuration)
                    .formattedTotalDuration(formatDuration(cabDuration))
                    .totalWaitingTimeMinutes(0)
                    .transferCount(0)
                    .travelerCount(travelers)
                    .costPerTraveler(cabPrice.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP))
                    .timelineSummary(origin + " → (Direct Cab) → " + destination)
                    .insights(List.of(
                            "0 Airport transfers or security lines required.",
                            "Fixed group price of ₹" + cabPrice + " split across " + travelers + " travelers (₹" +
                                    cabPrice.divide(BigDecimal.valueOf(travelers), 0, RoundingMode.HALF_UP) + "/person)."
                    ))
                    .build());
        }

        return options;
    }

    private JourneyOptionDto buildOption(
            String id, String title,
            String origin, String originAirport, String destAirport, String destination,
            String depRide1, String arrRide1, BigDecimal priceRide1, int durRide1, String provRide1, String iconRide1,
            int airportBufferMins,
            String depFlight, String arrFlight, BigDecimal flightPerSeat, int durFlight, String provFlight, String iconFlight,
            String depRide2, String arrRide2, BigDecimal priceRide2, int durRide2, String provRide2, String iconRide2,
            int travelers, List<String> insights) {

        int deboardBufferMins = 30;

        JourneySegmentDto seg1 = JourneySegmentDto.builder()
                .id(id + "-seg-1")
                .type(JourneySegmentType.RIDE_TO_AIRPORT)
                .typeName("Ride to Airport")
                .provider(provRide1)
                .origin(origin)
                .destination(originAirport)
                .departureTime(depRide1)
                .arrivalTime(arrRide1)
                .price(priceRide1)
                .durationMinutes(durRide1)
                .formattedDuration(formatDuration(durRide1))
                .icon(iconRide1)
                .metadata(Map.of("transferType", "Ground Transit", "service", provRide1))
                .build();

        JourneySegmentDto seg2 = JourneySegmentDto.builder()
                .id(id + "-seg-2")
                .type(JourneySegmentType.AIRPORT_BUFFER)
                .typeName("Airport Security & Gate Buffer")
                .provider("Terminal Security")
                .origin(originAirport)
                .destination(originAirport)
                .departureTime(arrRide1)
                .arrivalTime(depFlight)
                .price(BigDecimal.ZERO)
                .durationMinutes(airportBufferMins)
                .formattedDuration(formatDuration(airportBufferMins))
                .icon("⏳")
                .metadata(Map.of("bufferType", "Recommended Domestic Check-in"))
                .build();

        BigDecimal totalFlightCost = flightPerSeat.multiply(BigDecimal.valueOf(travelers));
        JourneySegmentDto seg3 = JourneySegmentDto.builder()
                .id(id + "-seg-3")
                .type(JourneySegmentType.FLIGHT)
                .typeName("Non-Stop Flight")
                .provider(provFlight)
                .origin(originAirport)
                .destination(destAirport)
                .departureTime(depFlight)
                .arrivalTime(arrFlight)
                .price(totalFlightCost)
                .durationMinutes(durFlight)
                .formattedDuration(formatDuration(durFlight))
                .icon(iconFlight)
                .metadata(Map.of("flightNumber", provFlight, "cabin", "Economy", "perSeat", "₹" + flightPerSeat))
                .build();

        JourneySegmentDto seg4 = JourneySegmentDto.builder()
                .id(id + "-seg-4")
                .type(JourneySegmentType.ARRIVAL_BUFFER)
                .typeName("Deboarding & Baggage Collection")
                .provider("Airport Arrival")
                .origin(destAirport)
                .destination(destAirport)
                .departureTime(arrFlight)
                .arrivalTime(depRide2)
                .price(BigDecimal.ZERO)
                .durationMinutes(deboardBufferMins)
                .formattedDuration(formatDuration(deboardBufferMins))
                .icon("🧳")
                .metadata(Map.of("bufferType", "Baggage Claim"))
                .build();

        JourneySegmentDto seg5 = JourneySegmentDto.builder()
                .id(id + "-seg-5")
                .type(JourneySegmentType.RIDE_FROM_AIRPORT)
                .typeName("Ride to Hotel / Destination")
                .provider(provRide2)
                .origin(destAirport)
                .destination(destination)
                .departureTime(depRide2)
                .arrivalTime(arrRide2)
                .price(priceRide2)
                .durationMinutes(durRide2)
                .formattedDuration(formatDuration(durRide2))
                .icon(iconRide2)
                .metadata(Map.of("transferType", "Final Destination Cab", "service", provRide2))
                .build();

        List<JourneySegmentDto> segments = List.of(seg1, seg2, seg3, seg4, seg5);

        BigDecimal totalCost = priceRide1.add(totalFlightCost).add(priceRide2);
        int totalDuration = durRide1 + airportBufferMins + durFlight + deboardBufferMins + durRide2;
        int totalWaiting = airportBufferMins + deboardBufferMins;

        return JourneyOptionDto.builder()
                .id(id)
                .title(title)
                .segments(segments)
                .totalCost(totalCost)
                .totalDurationMinutes(totalDuration)
                .formattedTotalDuration(formatDuration(totalDuration))
                .totalWaitingTimeMinutes(totalWaiting)
                .transferCount(2)
                .travelerCount(travelers)
                .costPerTraveler(totalCost.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP))
                .timelineSummary(origin + " → " + originAirport + " → " + destAirport + " → " + destination)
                .insights(insights)
                .build();
    }

    private String extractCity(String address, String defaultCity) {
        if (address == null || address.isBlank()) return defaultCity;
        String lower = address.toLowerCase();
        if (lower.contains("delhi")) return "Delhi";
        if (lower.contains("mumbai") || lower.contains("bombay")) return "Mumbai";
        if (lower.contains("goa")) return "Goa";
        if (lower.contains("bangalore") || lower.contains("bengaluru")) return "Bengaluru";
        if (lower.contains("jaipur")) return "Jaipur";
        if (lower.contains("bhopal")) return "Bhopal";
        return address.split(",")[0].trim();
    }

    private String formatDuration(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h > 0 && m > 0) {
            return String.format("%dh %02dm", h, m);
        } else if (h > 0) {
            return String.format("%dh", h);
        } else {
            return String.format("%dm", m);
        }
    }

    private String calculatePerPerson(BigDecimal ride1, BigDecimal flight, BigDecimal ride2, int travelers) {
        BigDecimal total = ride1.add(flight.multiply(BigDecimal.valueOf(travelers))).add(ride2);
        return String.format("%,d", total.divide(BigDecimal.valueOf(travelers), 0, RoundingMode.HALF_UP).longValue());
    }
}
