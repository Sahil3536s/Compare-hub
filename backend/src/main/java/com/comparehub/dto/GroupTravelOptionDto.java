package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTravelOptionDto {

    private String id;
    private GroupTransportMode mode;
    private String title;
    private String providerName;
    private BigDecimal totalCost;
    private BigDecimal costPerPerson;
    private int estimatedTravelTimeMinutes;
    private String formattedDuration;
    private int numberOfTravelers;
    private String vehicleCapacityNote;
    private int requiredConnections;

    @Builder.Default
    private List<GroupTravelLegDto> legs = new ArrayList<>();

    @Builder.Default
    private List<GroupCostItemDto> breakdown = new ArrayList<>();

    private double score; // 0-100
    private boolean isRecommended;
    private String recommendationReason;
    private String classification; // "BEST_GROUP_VALUE", "FASTEST_OPTION", "CHEAPEST_PER_PERSON", "BALANCED_CHOICE"
}
