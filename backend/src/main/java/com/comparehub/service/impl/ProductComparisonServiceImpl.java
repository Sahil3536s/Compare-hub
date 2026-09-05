package com.comparehub.service.impl;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.ComparisonRecommendationService;
import com.comparehub.service.ProductComparisonService;
import com.comparehub.service.ProductMatchingService;
import com.comparehub.service.ProductNormalizationService;
import com.comparehub.service.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductComparisonServiceImpl implements ProductComparisonService {

    private final List<ProductProvider> providers;
    private final ProductMatchingService productMatchingService;
    private final ProductNormalizationService normalizationService;
    private final ProductRankingService rankingService;
    private final ComparisonRecommendationService recommendationService;

    @Override
    @Cacheable(
            value = "product-comparisons",
            key = "T(java.lang.String).valueOf(#query).toLowerCase() + '_' + #merchant + '_' + #brand + '_' + #category + '_' + #minPrice + '_' + #maxPrice + '_' + #inStockOnly + '_' + #sortBy",
            unless = "#result == null || #result.offers.isEmpty()"
    )
    public ProductComparisonResponseDto compareProducts(
            String query,
            String merchant,
            String brand,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy) {

        log.info("Executing resilient product comparison search for query: '{}', total configured providers: {}", query, providers.size());

        // 1. Resiliently gather offers from all providers (failure of one does not affect others)
        List<NormalizedProductOfferDto> rawOffers = new ArrayList<>();
        for (ProductProvider provider : providers) {
            try {
                List<NormalizedProductOfferDto> providerResults = provider.searchProducts(query);
                if (providerResults != null && !providerResults.isEmpty()) {
                    rawOffers.addAll(providerResults);
                }
            } catch (Exception e) {
                log.error("Provider '{}' failed during search: {}. Continuing with remaining providers.",
                        provider.getProviderName(), e.getMessage());
            }
        }

        // 2. Intelligent Product Attribute Extraction & Matching
        List<NormalizedProductOfferDto> matchedOffers = productMatchingService.enrichWithMatching(rawOffers);

        // 3. Normalize offers
        List<NormalizedProductOfferDto> normalized = normalizationService.normalizeOffers(matchedOffers);

        // 4. Apply User Filters
        List<NormalizedProductOfferDto> filtered = normalized.stream()
                .filter(offer -> {
                    // Merchant filter
                    if (merchant != null && !merchant.isBlank() && !"all".equalsIgnoreCase(merchant)) {
                        if (!offer.getMerchant().equalsIgnoreCase(merchant.trim())) {
                            return false;
                        }
                    }
                    // Brand filter
                    if (brand != null && !brand.isBlank() && !"all".equalsIgnoreCase(brand)) {
                        if (!offer.getBrand().equalsIgnoreCase(brand.trim())) {
                            return false;
                        }
                    }
                    // Category filter
                    if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category) && !"all categories".equalsIgnoreCase(category)) {
                        if (!offer.getCategory().equalsIgnoreCase(category.trim())) {
                            return false;
                        }
                    }
                    // Min price filter
                    if (minPrice != null && offer.getPrice().compareTo(minPrice) < 0) {
                        return false;
                    }
                    // Max price filter
                    if (maxPrice != null && offer.getPrice().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    // In-stock filter
                    if (Boolean.TRUE.equals(inStockOnly) && Boolean.FALSE.equals(offer.getInStock())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 5. Apply Deterministic Ranking (Best Value, Price, Rating, Discount)
        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(filtered, sortBy);

        // 6. Find Overall Cheapest & Highest Rated
        BigDecimal cheapestPrice = null;
        String cheapestMerchant = null;

        if (!ranked.isEmpty()) {
            NormalizedProductOfferDto cheapest = ranked.stream()
                    .min(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                    .orElse(ranked.get(0));

            cheapestPrice = cheapest.getPrice();
            cheapestMerchant = cheapest.getMerchant();
        }

        // 7. Generate Structured AI Recommendation & Ranking Explanations
        AiRecommendationDto recommendation = recommendationService.recommendProducts(ranked);
        var rankingSummary = rankingService.getRankingSummary(ranked);

        return ProductComparisonResponseDto.builder()
                .query(query)
                .totalOffers(ranked.size())
                .cheapestPrice(cheapestPrice)
                .cheapestMerchant(cheapestMerchant)
                .offers(ranked)
                .aiRecommendation(recommendation)
                .rankingSummary(rankingSummary)
                .build();
    }
}
