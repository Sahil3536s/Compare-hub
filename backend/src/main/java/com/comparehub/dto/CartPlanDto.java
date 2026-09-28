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
public class CartPlanDto {

    private String planType; // "SINGLE_STORE" or "OPTIMIZED_MIXED"
    private String title;
    private String description;
    @Builder.Default
    private List<CartMerchantOrderDto> merchantOrders = new ArrayList<>();
    private BigDecimal totalProductsCost;
    private BigDecimal totalDeliveryFees;
    private BigDecimal totalPlatformFees;
    private BigDecimal grandTotal;
    private Integer totalOrders;
    @Builder.Default
    private Boolean isRecommended = false;
    @Builder.Default
    private BigDecimal savingsVsBaseline = BigDecimal.ZERO;
}
