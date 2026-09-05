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
public class CostTimeOptimizationResponseDto {

    private double costWeight;
    private double timeWeight;

    @Builder.Default
    private List<CostTimeOptionDto> rankedOptions = new ArrayList<>();

    private CostTimeOptionDto topOption;
    private CostTimeOptionDto cheapestOption;
    private CostTimeOptionDto fastestOption;
    private CostTimeOptionDto balancedOption;

    private String tradeoffSummary;
    private long executionTimeMs;
}
