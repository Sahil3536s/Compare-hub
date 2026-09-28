package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;
import com.comparehub.service.BudgetOptimizationService;
import com.comparehub.service.JourneyOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetOptimizationServiceImpl implements BudgetOptimizationService {

    private final JourneyOptimizationService journeyOptimizationService;

    @Override
    public BudgetOptimizationResponseDto optimizeBudgetPlan(BudgetConstraint constraint) {
        long startTime = System.currentTimeMillis();

        if (constraint == null) {
            constraint = BudgetConstraint.builder().build();
        }

        // If natural language query is provided, extract or enrich parameters
        if (constraint.getNaturalLanguageQuery() != null && !constraint.getNaturalLanguageQuery().isBlank()) {
            BudgetConstraint parsed = parseNaturalLanguageQuery(constraint.getNaturalLanguageQuery());
            if (constraint.getOrigin() == null || constraint.getOrigin().isBlank()) constraint.setOrigin(parsed.getOrigin());
            if (constraint.getDestination() == null || constraint.getDestination().isBlank()) constraint.setDestination(parsed.getDestination());
            if (constraint.getTravelers() <= 1 && parsed.getTravelers() > 1) constraint.setTravelers(parsed.getTravelers());
            if (constraint.getMaxBudget() == null && parsed.getMaxBudget() != null) constraint.setMaxBudget(parsed.getMaxBudget());
            if (constraint.getPreference() == null && parsed.getPreference() != null) constraint.setPreference(parsed.getPreference());
        }

        String origin = (constraint.getOrigin() != null && !constraint.getOrigin().isBlank())
                ? constraint.getOrigin().trim()
                : "Delhi";
        String destination = (constraint.getDestination() != null && !constraint.getDestination().isBlank())
                ? constraint.getDestination().trim()
                : "Goa";

        int travelers = Math.max(1, constraint.getTravelers());
        BigDecimal maxBudget = constraint.getMaxBudget() != null && constraint.getMaxBudget().compareTo(BigDecimal.ZERO) > 0
                ? constraint.getMaxBudget()
                : BigDecimal.valueOf(25000.00);

        BudgetPreference preference = constraint.getPreference() != null
                ? constraint.getPreference()
                : BudgetPreference.BALANCED;

        constraint.setOrigin(origin);
        constraint.setDestination(destination);
        constraint.setTravelers(travelers);
        constraint.setMaxBudget(maxBudget);
        constraint.setPreference(preference);

        // Fetch real combinations from Smart Journey 2.0 engine
        SmartJourneyRequestDto journeyRequest = SmartJourneyRequestDto.builder()
                .origin(origin)
                .destination(destination)
                .travelers(travelers)
                .airportBufferMinutes(constraint.getAirportBufferMinutes() > 0 ? constraint.getAirportBufferMinutes() : 90)
                .costWeight(preference == BudgetPreference.CHEAPEST ? 100.0 : (preference == BudgetPreference.FASTEST ? 0.0 : 50.0))
                .timeWeight(preference == BudgetPreference.FASTEST ? 100.0 : (preference == BudgetPreference.CHEAPEST ? 0.0 : 50.0))
                .build();

        SmartJourneyResponseDto journeyResponse = journeyOptimizationService.optimizeJourney(journeyRequest);
        List<JourneyOptionDto> allOptions = journeyResponse.getAllCombinations() != null
                ? journeyResponse.getAllCombinations()
                : new ArrayList<>();

        BigDecimal lowestAvailable = allOptions.stream()
                .map(JourneyOptionDto::getTotalCost)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<BudgetPlanOptionDto> validPlans = new ArrayList<>();
        int exceededCount = 0;

        for (JourneyOptionDto option : allOptions) {
            BigDecimal cost = option.getTotalCost();
            if (cost.compareTo(maxBudget) <= 0) {
                BigDecimal remaining = maxBudget.subtract(cost);
                double utilization = (cost.doubleValue() / maxBudget.doubleValue()) * 100.0;
                String comfort = determineComfortLevel(option);

                BudgetPlanOptionDto plan = BudgetPlanOptionDto.builder()
                        .id("plan-" + option.getId())
                        .title(option.getTitle())
                        .totalCost(cost)
                        .costPerTraveler(cost.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP))
                        .remainingBudget(remaining)
                        .budgetUtilizationPercent(Math.round(utilization * 10.0) / 10.0)
                        .durationMinutes(option.getTotalDurationMinutes())
                        .formattedDuration(option.getFormattedTotalDuration())
                        .transferCount(option.getTransferCount())
                        .comfortLevel(comfort)
                        .classification("FEASIBLE_MATCH")
                        .journeyOption(option)
                        .isWithinBudget(true)
                        .highlights(new ArrayList<>(option.getInsights() != null ? option.getInsights() : List.of()))
                        .build();

                validPlans.add(plan);
            } else {
                exceededCount++;
            }
        }

        // Identify Key Options
        BudgetPlanOptionDto cheapestPlan = validPlans.stream()
                .min(Comparator.comparing(BudgetPlanOptionDto::getTotalCost))
                .orElse(null);

        BudgetPlanOptionDto fastestPlan = validPlans.stream()
                .min(Comparator.comparingInt(BudgetPlanOptionDto::getDurationMinutes))
                .orElse(null);

        BudgetPlanOptionDto comfortPlan = validPlans.stream()
                .filter(p -> "HIGH".equals(p.getComfortLevel()))
                .findFirst()
                .orElse(fastestPlan);

        BudgetPlanOptionDto bestValuePlan = validPlans.stream()
                .filter(p -> p.getJourneyOption() != null && p.getJourneyOption().isBalanced())
                .findFirst()
                .orElse(validPlans.isEmpty() ? null : validPlans.get(0));

        // Assign classification labels and recommendations
        if (cheapestPlan != null) {
            cheapestPlan.setClassification("CHEAPEST");
            cheapestPlan.setRecommendationReason(String.format("Lowest cost itinerary at ₹%s (₹%s/person), leaving ₹%s in reserve.",
                    formatCurrency(cheapestPlan.getTotalCost()),
                    formatCurrency(cheapestPlan.getCostPerTraveler()),
                    formatCurrency(cheapestPlan.getRemainingBudget())));
        }

        if (fastestPlan != null && (cheapestPlan == null || !fastestPlan.getId().equals(cheapestPlan.getId()))) {
            fastestPlan.setClassification("FASTEST");
            fastestPlan.setRecommendationReason(String.format("Fastest door-to-door transit (%s) within budget, saving travel time.",
                    fastestPlan.getFormattedDuration()));
        }

        if (bestValuePlan != null && (cheapestPlan == null || !bestValuePlan.getId().equals(cheapestPlan.getId()))
                && (fastestPlan == null || !bestValuePlan.getId().equals(fastestPlan.getId()))) {
            bestValuePlan.setClassification("BEST_VALUE");
            bestValuePlan.setRecommendationReason(String.format("Optimal balance of convenience and price, utilizing %s%% of budget.",
                    bestValuePlan.getBudgetUtilizationPercent()));
        }

        // Determine best recommended plan based on user's preference
        BudgetPlanOptionDto bestPlan = switch (preference) {
            case CHEAPEST -> cheapestPlan != null ? cheapestPlan : bestValuePlan;
            case FASTEST -> fastestPlan != null ? fastestPlan : bestValuePlan;
            case COMFORT -> comfortPlan != null ? comfortPlan : fastestPlan;
            default -> bestValuePlan != null ? bestValuePlan : (cheapestPlan != null ? cheapestPlan : fastestPlan);
        };

        // Summary Text
        String summaryText;
        if (!validPlans.isEmpty()) {
            summaryText = String.format("Budget: ₹%s. Found %d valid itinerary option%s for %d traveler%s. %s is recommended (%s remaining).",
                    formatCurrency(maxBudget),
                    validPlans.size(),
                    validPlans.size() == 1 ? "" : "s",
                    travelers,
                    travelers == 1 ? "" : "s",
                    bestPlan != null ? bestPlan.getTitle() : "Option",
                    bestPlan != null ? "₹" + formatCurrency(bestPlan.getRemainingBudget()) : "₹0");
        } else {
            summaryText = String.format("No available travel combinations found under ₹%s for %d travelers. Lowest verified itinerary starts at ₹%s.",
                    formatCurrency(maxBudget), travelers, formatCurrency(lowestAvailable));
        }

        long execTime = System.currentTimeMillis() - startTime;

        return BudgetOptimizationResponseDto.builder()
                .constraint(constraint)
                .maxBudget(maxBudget)
                .plans(validPlans)
                .bestPlan(bestPlan)
                .cheapestPlan(cheapestPlan)
                .fastestPlan(fastestPlan)
                .bestValuePlan(bestValuePlan)
                .comfortPlan(comfortPlan)
                .exceededBudgetOptionsCount(exceededCount)
                .lowestAvailablePrice(lowestAvailable)
                .summaryText(summaryText)
                .executionTimeMs(execTime)
                .build();
    }

    @Override
    public BudgetConstraint parseNaturalLanguageQuery(String query) {
        if (query == null || query.isBlank()) {
            return BudgetConstraint.builder()
                    .origin("Delhi")
                    .destination("Goa")
                    .travelers(1)
                    .maxBudget(BigDecimal.valueOf(25000))
                    .preference(BudgetPreference.BALANCED)
                    .build();
        }

        String lower = query.toLowerCase().trim();

        // 1. Extract Travelers (e.g., "3 people", "3 travelers", "for 4", "2 of us")
        int travelers = 1;
        Pattern pTravelers = Pattern.compile("(\\d+)\\s*(?:people|persons|travelers|passengers|members|adults)");
        Matcher mTravelers = pTravelers.matcher(lower);
        if (mTravelers.find()) {
            travelers = Integer.parseInt(mTravelers.group(1));
        } else {
            Pattern pWeAre = Pattern.compile("(?:we are|group of|for)\\s*(\\d+)");
            Matcher mWeAre = pWeAre.matcher(lower);
            if (mWeAre.find()) {
                travelers = Integer.parseInt(mWeAre.group(1));
            }
        }

        // 2. Extract Budget (e.g., "budget is ₹25,000", "budget 25000", "under 30000", "upto 20k")
        BigDecimal maxBudget = BigDecimal.valueOf(25000);
        Pattern pBudget = Pattern.compile("(?:budget\\s*(?:is|of|around)?|under|upto|max(?:imum)?)\\s*(?:₹|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(k)?");
        Matcher mBudget = pBudget.matcher(lower);
        if (mBudget.find()) {
            String rawVal = mBudget.group(1).replace(",", "");
            double val = Double.parseDouble(rawVal);
            if ("k".equalsIgnoreCase(mBudget.group(2))) {
                val *= 1000.0;
            }
            maxBudget = BigDecimal.valueOf(val);
        } else {
            // Direct currency search
            Pattern pCurrency = Pattern.compile("(?:₹|rs\\.?|inr)\\s*([\\d,]+)");
            Matcher mCurr = pCurrency.matcher(lower);
            if (mCurr.find()) {
                maxBudget = BigDecimal.valueOf(Double.parseDouble(mCurr.group(1).replace(",", "")));
            }
        }

        // 3. Extract Origin & Destination (e.g. "going from Delhi to Goa", "Delhi to Mumbai")
        String origin = "Delhi";
        String destination = "Goa";

        Pattern pFromTo = Pattern.compile("(?:going\\s+from|from)\\s+([a-zA-Z\\s]+?)\\s+(?:to|towards)\\s+([a-zA-Z\\s]+?)(?=[.,]|\\bin\\b|\\bour\\b|\\bbudget\\b|\\bunder\\b|\\bfor\\b|\\bwith\\b|$)");
        Matcher mFromTo = pFromTo.matcher(lower);
        if (mFromTo.find()) {
            String o = mFromTo.group(1).trim();
            String d = mFromTo.group(2).trim();
            o = o.replaceAll("(?i)^(going|traveling|we are|we're|people|travelers|from)\\s*", "").trim();
            if (!o.isBlank()) origin = capitalize(o);
            if (!d.isBlank()) destination = capitalize(d);
        } else {
            Pattern pDirectTo = Pattern.compile("([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+(?:to|towards)\\s+([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)(?=[.,]|\\bin\\b|\\bour\\b|\\bbudget\\b|\\bunder\\b|\\bfor\\b|\\bwith\\b|$)");
            Matcher mDirectTo = pDirectTo.matcher(lower);
            if (mDirectTo.find()) {
                String o = mDirectTo.group(1).trim();
                String d = mDirectTo.group(2).trim();
                o = o.replaceAll("(?i)^(going|traveling|we are|we're|people|travelers|from)\\s*", "").trim();
                if (!o.isBlank()) origin = capitalize(o);
                if (!d.isBlank()) destination = capitalize(d);
            }
        }

        // 4. Extract Preference
        BudgetPreference preference = BudgetPreference.BALANCED;
        if (lower.contains("cheapest") || lower.contains("cheap") || lower.contains("save money") || lower.contains("lowest cost")) {
            preference = BudgetPreference.CHEAPEST;
        } else if (lower.contains("fastest") || lower.contains("quickest") || lower.contains("fast") || lower.contains("save time")) {
            preference = BudgetPreference.FASTEST;
        } else if (lower.contains("comfort") || lower.contains("premium") || lower.contains("luxury")) {
            preference = BudgetPreference.COMFORT;
        }

        return BudgetConstraint.builder()
                .origin(origin)
                .destination(destination)
                .travelers(travelers)
                .maxBudget(maxBudget)
                .preference(preference)
                .naturalLanguageQuery(query)
                .build();
    }

    @Override
    public BudgetOptimizationResponseDto getSampleBudgetPlan() {
        return optimizeBudgetPlan(BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(25000.00))
                .preference(BudgetPreference.BALANCED)
                .naturalLanguageQuery("We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.")
                .build());
    }

    private String determineComfortLevel(JourneyOptionDto option) {
        if (option == null || option.getTitle() == null) return "STANDARD";
        String title = option.getTitle().toLowerCase();
        if (title.contains("air india") || title.contains("prime") || title.contains("intercity")) {
            return "HIGH";
        } else if (title.contains("indigo") || title.contains("uber")) {
            return "MEDIUM";
        }
        return "STANDARD";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Arrays.stream(text.split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }
}
