package com.comparehub.service.impl;

import com.comparehub.dto.GroupTransportMode;
import com.comparehub.dto.GroupTravelOptionDto;
import com.comparehub.dto.GroupTravelRequestDto;
import com.comparehub.dto.GroupTravelResponseDto;
import com.comparehub.service.GroupTransportModeEvaluator;
import com.comparehub.service.GroupTravelRankingService;
import com.comparehub.service.GroupTravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupTravelServiceImpl implements GroupTravelService {

    private final List<GroupTransportModeEvaluator> evaluators;
    private final GroupTravelRankingService groupTravelRankingService;

    @Override
    public GroupTravelResponseDto optimizeGroupTravel(GroupTravelRequestDto request) {
        long startTime = System.currentTimeMillis();
        int travelers = Math.max(1, request.getNumberOfTravelers());

        log.info("Optimizing group travel for {} travelers from '{}' to '{}' (priority: {})",
                travelers, request.getOrigin(), request.getDestination(), request.getPriority());

        List<GroupTravelOptionDto> rawOptions = new ArrayList<>();
        for (GroupTransportModeEvaluator evaluator : evaluators) {
            try {
                List<GroupTravelOptionDto> evaluated = evaluator.evaluate(request);
                if (evaluated != null) {
                    rawOptions.addAll(evaluated);
                }
            } catch (Exception e) {
                log.error("Evaluator for mode '{}' failed: {}. Continuing with other modes.",
                        evaluator.getSupportedMode(), e.getMessage());
            }
        }

        // Apply budget constraint if specified
        List<GroupTravelOptionDto> filteredOptions = rawOptions;
        if (request.getBudget() != null && request.getBudget() > 0) {
            filteredOptions = rawOptions.stream()
                    .filter(o -> o.getTotalCost().doubleValue() <= request.getBudget())
                    .toList();
            if (filteredOptions.isEmpty()) {
                // If all exceed budget, fallback to raw so user sees options with closest price
                filteredOptions = rawOptions;
            }
        }

        // Rank options based on user priority
        List<GroupTravelOptionDto> rankedOptions = groupTravelRankingService.rankOptions(
                filteredOptions, request.getPriority(), travelers);

        GroupTravelOptionDto bestValue = rankedOptions.stream()
                .filter(GroupTravelOptionDto::isRecommended)
                .findFirst()
                .orElse(rankedOptions.isEmpty() ? null : rankedOptions.get(0));

        GroupTravelOptionDto cheapest = rankedOptions.stream()
                .min(Comparator.comparing(GroupTravelOptionDto::getTotalCost))
                .orElse(null);

        GroupTravelOptionDto fastest = rankedOptions.stream()
                .min(Comparator.comparingInt(GroupTravelOptionDto::getEstimatedTravelTimeMinutes))
                .orElse(null);

        String insights = generateGroupInsights(rankedOptions, travelers, cheapest, fastest);

        long executionTimeMs = System.currentTimeMillis() - startTime;

        return GroupTravelResponseDto.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .travelDate(request.getTravelDate())
                .numberOfTravelers(travelers)
                .budget(request.getBudget())
                .priority(request.getPriority() != null ? request.getPriority() : "BALANCED")
                .options(rankedOptions)
                .bestValueOption(bestValue)
                .cheapestOption(cheapest)
                .fastestOption(fastest)
                .groupInsights(insights)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    private String generateGroupInsights(List<GroupTravelOptionDto> options, int travelers,
                                         GroupTravelOptionDto cheapest, GroupTravelOptionDto fastest) {
        if (options == null || options.isEmpty()) {
            return "No valid transport options available for the requested route.";
        }

        GroupTravelOptionDto flightOption = options.stream()
                .filter(o -> o.getMode() == GroupTransportMode.FLIGHT_AND_RIDE)
                .findFirst()
                .orElse(null);

        GroupTravelOptionDto directRideOption = options.stream()
                .filter(o -> o.getMode() == GroupTransportMode.DIRECT_RIDE)
                .findFirst()
                .orElse(null);

        if (flightOption != null && directRideOption != null) {
            BigDecimal flightCost = flightOption.getTotalCost();
            BigDecimal cabCost = directRideOption.getTotalCost();

            if (cabCost.compareTo(flightCost) < 0) {
                BigDecimal savings = flightCost.subtract(cabCost);
                return String.format("For %d traveler(s), hiring a Direct Cab saves ₹%s overall (₹%s/person vs ₹%s/person by air) because vehicle cost is shared across all passengers.",
                        travelers, savings.toPlainString(), directRideOption.getCostPerPerson().toPlainString(), flightOption.getCostPerPerson().toPlainString());
            } else {
                BigDecimal diff = cabCost.subtract(flightCost);
                return String.format("For %d traveler(s), Flight + Cabs is ₹%s cheaper and saves %d minutes compared to road travel.",
                        travelers, diff.toPlainString(), directRideOption.getEstimatedTravelTimeMinutes() - flightOption.getEstimatedTravelTimeMinutes());
            }
        }

        if (cheapest != null) {
            return String.format("Cheapest option for %d traveler(s) is %s at ₹%s total (₹%s per person).",
                    travelers, cheapest.getTitle(), cheapest.getTotalCost().toPlainString(), cheapest.getCostPerPerson().toPlainString());
        }

        return String.format("Calculated group travel comparisons across %d options for %d traveler(s).",
                options.size(), travelers);
    }
}
