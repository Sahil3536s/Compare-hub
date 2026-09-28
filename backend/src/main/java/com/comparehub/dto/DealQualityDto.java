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
public class DealQualityDto {

    private Integer dealScore; // 0 to 100
    private String classification; // "EXCEPTIONAL_DEAL", "GREAT_DEAL", "GOOD_DEAL", "AVERAGE_PRICE", "EXPENSIVE"
    private String classificationLabel; // "Exceptional Deal", "Great Deal", "Good Deal", "Normal Market Price", "Above Average Price"

    private BigDecimal currentPrice;
    private BigDecimal historicalAverage;
    private BigDecimal thirtyDayLow;
    private BigDecimal ninetyDayLow;
    private BigDecimal historicalMedian;

    private BigDecimal advertisedOriginalPrice;
    private Integer advertisedDiscountPercent;
    private Integer realDiscountPercentVsAverage;
    private BigDecimal realSavingsVsAverage;

    private String summary;
    private String disclaimer;
    @Builder.Default
    private Boolean isAtLowest = false;
}
