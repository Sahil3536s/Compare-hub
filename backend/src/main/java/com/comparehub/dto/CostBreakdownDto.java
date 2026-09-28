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
public class CostBreakdownDto {

    private BigDecimal basePrice;
    private BigDecimal deliveryFee;
    private BigDecimal platformFee;
    private BigDecimal otherFees;
    private BigDecimal discounts;
    private BigDecimal effectivePrice;
    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private List<AppliedOfferDto> appliedOffers = new ArrayList<>();

    @Builder.Default
    private Boolean hasConditionalDiscounts = false;

    private PaymentOfferSummaryDto paymentOffers;
}
