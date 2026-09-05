package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTravelResponseDto {

    private String origin;
    private String destination;
    private String travelDate;
    private int numberOfTravelers;
    private Double budget;
    private String priority;

    @Builder.Default
    private List<GroupTravelOptionDto> options = new ArrayList<>();

    private GroupTravelOptionDto bestValueOption;
    private GroupTravelOptionDto fastestOption;
    private GroupTravelOptionDto cheapestOption;

    private String groupInsights;
    private long executionTimeMs;
}
