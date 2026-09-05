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
public class FeatureComparisonDto {

    private BigDecimal priceDiff; // signed: altPrice - basePrice (negative means cheaper)
    private Double priceDiffPercent; // percentage difference
    private Boolean isCheaper;
    private Double ratingDiff; // altRating - baseRating
    private String ramComparison; // e.g. "16GB vs 12GB (+4GB)"
    private String storageComparison; // e.g. "512GB vs 256GB (2x Storage)"
    private String screenSizeComparison; // e.g. "6.7 inch vs 6.1 inch"
    private String processorComparison;
    private Integer betterSpecsCount;
}
