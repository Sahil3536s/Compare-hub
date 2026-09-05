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
public class ProductAlternativeDto {

    private String id;
    private String productName;
    private String brand;
    private String category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal priceDifference; // e.g. -8000.00
    private Double priceDifferencePercent; // e.g. -11.4
    private String imageUrl;
    private Double rating;
    private String merchant;
    private String productUrl;
    private AlternativeCategoryType categoryType; // CHEAPER_ALTERNATIVE, BETTER_VALUE, SIMILAR_PRICE_BETTER_FEATURE, PREMIUM_ALTERNATIVE
    private String categoryLabel; // "Cheaper Option", "Best Value Pick", "Feature Upgrade", "Premium Option"
    private Integer similarityScore; // 0 - 100
    @Builder.Default
    private List<String> highlights = new ArrayList<>();
    private FeatureComparisonDto featureComparison;
    private ProductAttributesDto attributes;
}
