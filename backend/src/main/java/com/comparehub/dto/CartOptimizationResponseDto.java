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
public class CartOptimizationResponseDto {

    private String strategy;
    private Integer totalItemsRequested;
    private Integer totalItemsMatched;
    @Builder.Default
    private List<CartPlanDto> singleStorePlans = new ArrayList<>();
    private CartPlanDto cheapestSingleStore;
    private CartPlanDto optimizedMixedPlan;
    private CartPlanDto recommendedPlan;
    @Builder.Default
    private BigDecimal estimatedSavings = BigDecimal.ZERO;
    private String explanation;
}
