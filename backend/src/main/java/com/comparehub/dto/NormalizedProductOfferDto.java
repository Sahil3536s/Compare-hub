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
public class NormalizedProductOfferDto {

    private String productName;
    private String merchant;
    private BigDecimal price; // Listed / Base Price
    private BigDecimal originalPrice;
    @Builder.Default
    private String currency = "INR";
    private Double rating;
    private String delivery;
    private String imageUrl;
    private String productUrl;
    @Builder.Default
    private Boolean inStock = true;
    private Integer discountPercent;
    @Builder.Default
    private Boolean isCheapest = false;
    @Builder.Default
    private Boolean isBestValue = false;
    @Builder.Default
    private Boolean isHighestRated = false;
    private Double rankingScore;
    private String brand;
    private String category;

    // Phase 24: Intelligent Matching & Attributes
    private ProductAttributesDto attributes;
    private String canonicalKey;

    // Phase 25: True Cost Engine
    private BigDecimal effectivePrice;
    private CostBreakdownDto costBreakdown;

    // Phase 28: Deal Quality Engine
    private DealQualityDto dealQuality;

    // Phase 29: Purchase Timing Engine
    private PurchaseTimingDto purchaseTiming;

    // Phase 30: Payment Offer Optimization
    private PaymentOfferSummaryDto paymentOffers;
    private BigDecimal bestPaymentPrice;
}
