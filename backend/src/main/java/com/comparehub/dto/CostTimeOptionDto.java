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
public class CostTimeOptionDto {

    private String id;
    private String title;
    private String category; // "FLIGHT", "RIDE", "GROUP_TRAVEL", "GENERAL"
    private BigDecimal cost;
    private int durationMinutes;
    private String formattedDuration;

    private double costScore; // 0-100 (higher means lower cost)
    private double timeScore; // 0-100 (higher means faster)
    private double compositeScore; // 0-100

    private int rank;
    private String classification; // "CHEAPEST", "FASTEST", "BALANCED", "TOP_MATCH"
    private String tradeoffExplanation;
}
