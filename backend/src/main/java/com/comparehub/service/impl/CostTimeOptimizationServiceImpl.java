package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.service.CostTimeOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class CostTimeOptimizationServiceImpl implements CostTimeOptimizationService {

    @Override
    public CostTimeOptimizationResponseDto optimize(CostTimeOptimizationRequestDto request) {
        long startTime = System.currentTimeMillis();

        double costWeight = request.getCostWeight();
        double timeWeight = request.getTimeWeight();

        // Ensure weights are normalized
        if (costWeight + timeWeight > 0) {
            double total = costWeight + timeWeight;
            costWeight = (costWeight / total) * 100.0;
            timeWeight = 100.0 - costWeight;
        } else {
            costWeight = 50.0;
            timeWeight = 50.0;
        }

        List<CostTimeOptionDto> rankedOptions = rankOptions(request.getOptions(), costWeight);

        CostTimeOptionDto top = rankedOptions.isEmpty() ? null : rankedOptions.get(0);

        CostTimeOptionDto cheapest = rankedOptions.stream()
                .min(Comparator.comparing(CostTimeOptionDto::getCost))
                .orElse(null);

        CostTimeOptionDto fastest = rankedOptions.stream()
                .min(Comparator.comparingInt(CostTimeOptionDto::getDurationMinutes))
                .orElse(null);

        // Calculate 50/50 balanced option
        List<CostTimeOptionDto> balancedRanked = rankOptions(request.getOptions(), 50.0);
        CostTimeOptionDto balanced = balancedRanked.isEmpty() ? null : balancedRanked.get(0);

        String tradeoffSummary = generateTradeoffInsight(cheapest, fastest, top, costWeight);

        long executionTimeMs = System.currentTimeMillis() - startTime;

        return CostTimeOptimizationResponseDto.builder()
                .costWeight(Math.round(costWeight * 10.0) / 10.0)
                .timeWeight(Math.round(timeWeight * 10.0) / 10.0)
                .rankedOptions(rankedOptions)
                .topOption(top)
                .cheapestOption(cheapest)
                .fastestOption(fastest)
                .balancedOption(balanced)
                .tradeoffSummary(tradeoffSummary)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    @Override
    public List<CostTimeOptionDto> rankOptions(List<CostTimeOptionDto> options, double costWeight) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        double wCost = Math.max(0.0, Math.min(100.0, costWeight)) / 100.0;
        double wTime = 1.0 - wCost;

        double minCost = options.stream().mapToDouble(o -> o.getCost().doubleValue()).min().orElse(1.0);
        double maxCost = options.stream().mapToDouble(o -> o.getCost().doubleValue()).max().orElse(1.0);

        int minTime = options.stream().mapToInt(CostTimeOptionDto::getDurationMinutes).min().orElse(1);
        int maxTime = options.stream().mapToInt(CostTimeOptionDto::getDurationMinutes).max().orElse(1);

        List<CostTimeOptionDto> result = new ArrayList<>();

        for (CostTimeOptionDto opt : options) {
            double cost = opt.getCost().doubleValue();
            double costScore = (maxCost == minCost)
                    ? 100.0
                    : ((maxCost - cost) / (maxCost - minCost)) * 100.0;

            int time = opt.getDurationMinutes();
            double timeScore = (maxTime == minTime)
                    ? 100.0
                    : ((double) (maxTime - time) / (maxTime - minTime)) * 100.0;

            double compositeScore = (costScore * wCost) + (timeScore * wTime);

            int hours = time / 60;
            int mins = time % 60;
            String formattedDur = hours > 0 ? String.format("%dh %02dm", hours, mins) : String.format("%dm", mins);

            CostTimeOptionDto scored = CostTimeOptionDto.builder()
                    .id(opt.getId())
                    .title(opt.getTitle())
                    .category(opt.getCategory())
                    .cost(opt.getCost())
                    .durationMinutes(time)
                    .formattedDuration(formattedDur)
                    .costScore(Math.round(costScore * 10.0) / 10.0)
                    .timeScore(Math.round(timeScore * 10.0) / 10.0)
                    .compositeScore(Math.round(compositeScore * 10.0) / 10.0)
                    .build();

            result.add(scored);
        }

        // Sort descending by composite score, then by cost, then duration
        result.sort((a, b) -> {
            int scoreCmp = Double.compare(b.getCompositeScore(), a.getCompositeScore());
            if (scoreCmp != 0) return scoreCmp;
            int costCmp = a.getCost().compareTo(b.getCost());
            if (costCmp != 0) return costCmp;
            return Integer.compare(a.getDurationMinutes(), b.getDurationMinutes());
        });

        // Assign ranks and classifications
        for (int i = 0; i < result.size(); i++) {
            CostTimeOptionDto item = result.get(i);
            item.setRank(i + 1);

            if (i == 0) {
                if (wCost >= 0.85) {
                    item.setClassification("CHEAPEST");
                    item.setTradeoffExplanation("Ranked #1 for maximizing budget savings (₹" + item.getCost() + ").");
                } else if (wCost <= 0.15) {
                    item.setClassification("FASTEST");
                    item.setTradeoffExplanation("Ranked #1 for fastest transit duration (" + item.getFormattedDuration() + ").");
                } else if (wCost >= 0.40 && wCost <= 0.60) {
                    item.setClassification("BALANCED");
                    item.setTradeoffExplanation("Ranked #1 for best optimal balance between price (₹" + item.getCost() + ") and duration (" + item.getFormattedDuration() + ").");
                } else {
                    item.setClassification("TOP_MATCH");
                    item.setTradeoffExplanation("Top match for custom preference (" + Math.round(costWeight) + "% Money / " + Math.round(100 - costWeight) + "% Time).");
                }
            }
        }

        return result;
    }

    @Override
    public List<NormalizedFlightOfferDto> optimizeFlights(List<NormalizedFlightOfferDto> flights, double costWeight) {
        if (flights == null || flights.isEmpty()) return new ArrayList<>();

        List<CostTimeOptionDto> options = new ArrayList<>();
        Map<String, NormalizedFlightOfferDto> flightMap = new HashMap<>();

        for (int i = 0; i < flights.size(); i++) {
            NormalizedFlightOfferDto f = flights.get(i);
            String id = "flight_" + i + "_" + f.getAirline() + "_" + f.getFlightNumber();
            flightMap.put(id, f);

            options.add(CostTimeOptionDto.builder()
                    .id(id)
                    .title(f.getAirline() + " " + f.getFlightNumber())
                    .category("FLIGHT")
                    .cost(f.getPrice())
                    .durationMinutes(f.getDurationMinutes())
                    .build());
        }

        List<CostTimeOptionDto> ranked = rankOptions(options, costWeight);
        List<NormalizedFlightOfferDto> result = new ArrayList<>();

        for (int i = 0; i < ranked.size(); i++) {
            CostTimeOptionDto r = ranked.get(i);
            NormalizedFlightOfferDto orig = flightMap.get(r.getId());
            if (orig != null) {
                orig.setScore(r.getCompositeScore());
                orig.setIsBest(i == 0);
                result.add(orig);
            }
        }

        return result;
    }

    @Override
    public List<NormalizedRideOfferDto> optimizeRides(List<NormalizedRideOfferDto> rides, double costWeight) {
        if (rides == null || rides.isEmpty()) return new ArrayList<>();

        List<CostTimeOptionDto> options = new ArrayList<>();
        Map<String, NormalizedRideOfferDto> rideMap = new HashMap<>();

        for (int i = 0; i < rides.size(); i++) {
            NormalizedRideOfferDto r = rides.get(i);
            String id = "ride_" + i + "_" + r.getProvider() + "_" + r.getRideType();
            rideMap.put(id, r);

            BigDecimal fare = r.getEstimatedPriceMin() != null
                    ? r.getEstimatedPriceMin()
                    : (r.getEstimatedPriceMax() != null ? r.getEstimatedPriceMax() : BigDecimal.valueOf(500));

            int duration = r.getEtaMinutes() != null ? r.getEtaMinutes() : 20;

            options.add(CostTimeOptionDto.builder()
                    .id(id)
                    .title(r.getProvider() + " " + r.getRideType())
                    .category("RIDE")
                    .cost(fare)
                    .durationMinutes(duration)
                    .build());
        }

        List<CostTimeOptionDto> ranked = rankOptions(options, costWeight);
        List<NormalizedRideOfferDto> result = new ArrayList<>();

        for (int i = 0; i < ranked.size(); i++) {
            CostTimeOptionDto r = ranked.get(i);
            NormalizedRideOfferDto orig = rideMap.get(r.getId());
            if (orig != null) {
                orig.setScore(r.getCompositeScore());
                orig.setIsBest(i == 0);
                result.add(orig);
            }
        }

        return result;
    }

    @Override
    public List<GroupTravelOptionDto> optimizeGroupTravel(List<GroupTravelOptionDto> groupOptions, double costWeight) {
        if (groupOptions == null || groupOptions.isEmpty()) return new ArrayList<>();

        List<CostTimeOptionDto> options = new ArrayList<>();
        Map<String, GroupTravelOptionDto> groupMap = new HashMap<>();

        for (GroupTravelOptionDto g : groupOptions) {
            groupMap.put(g.getId(), g);
            options.add(CostTimeOptionDto.builder()
                    .id(g.getId())
                    .title(g.getTitle())
                    .category("GROUP_TRAVEL")
                    .cost(g.getTotalCost())
                    .durationMinutes(g.getEstimatedTravelTimeMinutes())
                    .build());
        }

        List<CostTimeOptionDto> ranked = rankOptions(options, costWeight);
        List<GroupTravelOptionDto> result = new ArrayList<>();

        for (int i = 0; i < ranked.size(); i++) {
            CostTimeOptionDto r = ranked.get(i);
            GroupTravelOptionDto orig = groupMap.get(r.getId());
            if (orig != null) {
                orig.setScore(r.getCompositeScore());
                orig.setRecommended(i == 0);
                if (i == 0) {
                    orig.setClassification(r.getClassification());
                    orig.setRecommendationReason(r.getTradeoffExplanation());
                }
                result.add(orig);
            }
        }

        return result;
    }

    @Override
    public String generateTradeoffInsight(CostTimeOptionDto cheapest, CostTimeOptionDto fastest,
                                          CostTimeOptionDto top, double costWeight) {
        if (cheapest == null || fastest == null || top == null) {
            return "Evaluating cost vs time tradeoff.";
        }

        if (cheapest.getId().equals(fastest.getId())) {
            return String.format("%s is both the cheapest (₹%s) and fastest (%s) option available.",
                    cheapest.getTitle(), cheapest.getCost(), cheapest.getFormattedDuration());
        }

        BigDecimal costDiff = fastest.getCost().subtract(cheapest.getCost());
        int timeDiffMinutes = cheapest.getDurationMinutes() - fastest.getDurationMinutes();
        int savedHours = timeDiffMinutes / 60;
        int savedMins = timeDiffMinutes % 60;
        String timeDiffStr = savedHours > 0 ? String.format("%dh %02dm", savedHours, savedMins) : String.format("%dm", savedMins);

        if (top.getId().equals(cheapest.getId())) {
            return String.format("Choosing %s saves ₹%s compared to %s, adding %s transit time.",
                    cheapest.getTitle(), costDiff, fastest.getTitle(), timeDiffStr);
        } else if (top.getId().equals(fastest.getId())) {
            return String.format("Choosing %s saves %s travel time for an additional ₹%s over %s.",
                    fastest.getTitle(), timeDiffStr, costDiff, cheapest.getTitle());
        } else {
            BigDecimal topVsCheapCost = top.getCost().subtract(cheapest.getCost());
            int topVsCheapTimeSaved = cheapest.getDurationMinutes() - top.getDurationMinutes();
            int h = topVsCheapTimeSaved / 60;
            int m = topVsCheapTimeSaved % 60;
            String savedStr = h > 0 ? String.format("%dh %02dm", h, m) : String.format("%dm", m);

            return String.format("Balanced pick: %s saves %s transit time for only ₹%s extra compared to the cheapest option.",
                    top.getTitle(), savedStr, topVsCheapCost);
        }
    }
}
