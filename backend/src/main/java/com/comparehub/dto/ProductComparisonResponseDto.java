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
}
