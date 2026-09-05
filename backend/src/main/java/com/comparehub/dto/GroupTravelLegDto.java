package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTravelLegDto {

    private String legType; // "RIDE", "FLIGHT", "TRAIN", "BUS"
    private String title;
    private String origin;
    private String destination;
    private int durationMinutes;
    private BigDecimal cost;
    private String provider;
    private String notes;
}
