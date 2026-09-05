package com.comparehub.service.impl;

import com.comparehub.dto.GroupTransportMode;
import com.comparehub.dto.GroupTravelOptionDto;
import com.comparehub.service.GroupTravelRankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class GroupTravelRankingServiceImpl implements GroupTravelRankingService {

    @Override
    public List<GroupTravelOptionDto> rankOptions(List<GroupTravelOptionDto> options, String priority, int numberOfTravelers) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        String pref = priority != null ? priority.trim().toUpperCase() : "BALANCED";

        double priceWeight = 0.45;
        double timeWeight = 0.40;
        double connWeight = 0.15;

        if ("CHEAPEST".equals(pref) || "PRICE".equals(pref)) {
            priceWeight = 0.80;
            timeWeight = 0.10;
            connWeight = 0.10;
        } else if ("FASTEST".equals(pref) || "SPEED".equals(pref)) {
            priceWeight = 0.15;
            timeWeight = 0.75;
            connWeight = 0.10;
        }

        double minCost = options.stream().mapToDouble(o -> o.getTotalCost().doubleValue()).min().orElse(1.0);
        double maxCost = options.stream().mapToDouble(o -> o.getTotalCost().doubleValue()).max().orElse(1.0);

        int minTime = options.stream().mapToInt(GroupTravelOptionDto::getEstimatedTravelTimeMinutes).min().orElse(1);
        int maxTime = options.stream().mapToInt(GroupTravelOptionDto::getEstimatedTravelTimeMinutes).max().orElse(1);

        for (GroupTravelOptionDto opt : options) {
            double cost = opt.getTotalCost().doubleValue();
            double priceScore = (maxCost == minCost) ? 100.0 : ((maxCost - cost) / (maxCost - minCost)) * 100.0;

            int time = opt.getEstimatedTravelTimeMinutes();
            double timeScore = (maxTime == minTime) ? 100.0 : ((double) (maxTime - time) / (maxTime - minTime)) * 100.0;

            double connScore = opt.getRequiredConnections() == 0 ? 100.0 : (opt.getRequiredConnections() == 1 ? 70.0 : 40.0);

            double compositeScore = (priceScore * priceWeight) + (timeScore * timeWeight) + (connScore * connWeight);
            opt.setScore(Math.round(compositeScore * 10.0) / 10.0);
            opt.setRecommended(false);
        }

        // Sort descending by score
        List<GroupTravelOptionDto> sorted = new ArrayList<>(options);
        sorted.sort(Comparator.comparingDouble(GroupTravelOptionDto::getScore).reversed());

        if (!sorted.isEmpty()) {
            GroupTravelOptionDto top = sorted.get(0);
            top.setRecommended(true);

            if ("CHEAPEST".equals(pref)) {
                top.setClassification("BEST_GROUP_VALUE");
                top.setRecommendationReason(String.format("Lowest overall cost for %d traveler(s) at ₹%s (₹%s/person).",
                        numberOfTravelers, top.getTotalCost().toPlainString(), top.getCostPerPerson().toPlainString()));
            } else if ("FASTEST".equals(pref)) {
                top.setClassification("FASTEST_OPTION");
                top.setRecommendationReason(String.format("Shortest travel time (%s) for the group.", top.getFormattedDuration()));
            } else {
                top.setClassification("BALANCED_CHOICE");
                if (top.getMode() == GroupTransportMode.DIRECT_RIDE && numberOfTravelers >= 3) {
                    top.setRecommendationReason(String.format("Optimal balance: Direct door-to-door cab saves money for %d people (₹%s/person) with 0 connections.",
                            numberOfTravelers, top.getCostPerPerson().toPlainString()));
                } else if (top.getMode() == GroupTransportMode.FLIGHT_AND_RIDE) {
                    top.setRecommendationReason(String.format("Optimal balance: Fastest travel duration (%s) at competitive group airfare.",
                            top.getFormattedDuration()));
                } else {
                    top.setRecommendationReason(String.format("Best overall score balancing group cost (₹%s/person) and travel time (%s).",
                            top.getCostPerPerson().toPlainString(), top.getFormattedDuration()));
                }
            }
        }

        return sorted;
    }
}
