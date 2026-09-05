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
public class ProductPriceHistoryResponseDto {

    private Long productId;
    private String productName;
    private String period; // "7D", "30D", "90D"
    private BigDecimal currentPrice;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private BigDecimal averagePrice;
    private String currency;
    private String analysisText;
    @Builder.Default
    private List<PricePointDto> pricePoints = new ArrayList<>();

    // Phase 28: Deal Quality
    private DealQualityDto dealQuality;

    // Phase 29: Purchase Timing
    private PurchaseTimingDto purchaseTiming;
}
