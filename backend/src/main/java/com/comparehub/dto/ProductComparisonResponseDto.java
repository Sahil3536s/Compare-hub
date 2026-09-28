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
public class ProductComparisonResponseDto {

    private String query;
    private Integer totalOffers;
    private BigDecimal cheapestPrice;
    private String cheapestMerchant;
    @Builder.Default
    private List<NormalizedProductOfferDto> offers = new ArrayList<>();
    private AiRecommendationDto aiRecommendation;
    private RankingSummaryDto rankingSummary;

    @Builder.Default
    private List<ProductAlternativeDto> alternatives = new ArrayList<>();

    @Builder.Default
    private String status = "SUCCESS"; // "SUCCESS", "PARTIAL_SUCCESS", "FAILED"

    private String statusMessage;

    @Builder.Default
    private List<String> successfulProviders = new ArrayList<>();

    @Builder.Default
    private List<String> failedProviders = new ArrayList<>();

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer pageSize = 20;

    @Builder.Default
    private Integer totalPages = 1;

    @Builder.Default
    private Boolean hasMore = false;

    @Builder.Default
    private java.util.Map<String, List<String>> dynamicFilters = new java.util.LinkedHashMap<>();

    @Builder.Default
    private List<String> availableCategories = new ArrayList<>();

    @Builder.Default
    private List<String> availableBrands = new ArrayList<>();

    @Builder.Default
    private List<String> availableMerchants = new ArrayList<>();
}
