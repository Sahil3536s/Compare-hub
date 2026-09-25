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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductComparisonServiceImpl implements ProductComparisonService {

    private final List<ProductProvider> providers;
    private final ProductMatchingService productMatchingService;
    private final ProductNormalizationService normalizationService;
    private final ProductRankingService rankingService;
    private final ComparisonRecommendationService recommendationService;
    private final Executor providerExecutor;

    @Autowired
    public ProductComparisonServiceImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService,
            @Qualifier("providerExecutor") Executor providerExecutor) {
        this.providers = providers;
        this.productMatchingService = productMatchingService;
        this.normalizationService = normalizationService;
        this.rankingService = rankingService;
        this.recommendationService = recommendationService;
        this.providerExecutor = providerExecutor;
    }

    // Overloaded constructor for tests or default executor
    public ProductComparisonServiceImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService) {
        this(providers, productMatchingService, normalizationService, rankingService, recommendationService, ForkJoinPool.commonPool());
    }

    @Override
    public ProductComparisonResponseDto compareProducts(
            String query,
            String merchant,
            String brand,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy) {
        return compareProducts(query, merchant, brand, category, minPrice, maxPrice, inStockOnly, sortBy, null, null, null, null);
    }

    @Override
    @Cacheable(
            value = "product-comparisons",
            key = "T(java.lang.String).valueOf(#query).toLowerCase() + '_' + #merchant + '_' + #brand + '_' + #category + '_' + #minPrice + '_' + #maxPrice + '_' + #inStockOnly + '_' + #sortBy + '_' + #minRating + '_' + #ram + '_' + #storage + '_' + #delivery",
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
            String sortBy,
            Double minRating,
            String ram,
            String storage,
            String delivery) {

        log.info("Executing resilient product comparison search for query: '{}', total configured providers: {}", query, providers.size());

        List<String> failedProviders = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<String> successfulProviders = new java.util.concurrent.CopyOnWriteArrayList<>();

        // 1. Resiliently gather offers from all providers in PARALLEL using dedicated bounded executor
        List<CompletableFuture<List<NormalizedProductOfferDto>>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<NormalizedProductOfferDto> results = provider.searchProducts(query);
                        if (results != null) {
                            successfulProviders.add(provider.getProviderName());
                            return results;
                        }
                        successfulProviders.add(provider.getProviderName());
                        return List.<NormalizedProductOfferDto>of();
                    } catch (Exception e) {
                        log.error("Provider '{}' failed during search: {}. Continuing with remaining providers.",
                                provider.getProviderName(), e.getMessage());
                        failedProviders.add(provider.getProviderName());
                        return List.<NormalizedProductOfferDto>of();
                    }
                }, providerExecutor)
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("Provider timed out or failed: {}", ex.getMessage());
                    failedProviders.add(provider.getProviderName());
                    return List.of();
                }))
                .toList();

        List<NormalizedProductOfferDto> rawOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));

        // 2. Data Normalization & Attribute Extraction (modular price/currency/brand/attributes normalization)
        List<NormalizedProductOfferDto> normalized = normalizationService.normalizeOffers(rawOffers);

        // 3. Product Matching & Canonical Offer Grouping (strict variant safety)
        List<NormalizedProductOfferDto> matchedOffers = productMatchingService.enrichWithMatching(normalized);

        // 4. Apply User Filters
        List<NormalizedProductOfferDto> filtered = matchedOffers.stream()
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
                    // Minimum Rating filter
                    if (minRating != null && minRating > 0) {
                        if (offer.getRating() == null || offer.getRating() < minRating) {
                            return false;
                        }
                    }
                    // RAM filter
                    if (ram != null && !ram.isBlank() && !"all".equalsIgnoreCase(ram)) {
                        String cleanRam = ram.toLowerCase().replaceAll("\\s+", "");
                        String offerRam = offer.getAttributes() != null && offer.getAttributes().getRam() != null
                                ? offer.getAttributes().getRam().toLowerCase().replaceAll("\\s+", "")
                                : "";
                        String productName = offer.getProductName() != null ? offer.getProductName().toLowerCase() : "";
                        if (!offerRam.contains(cleanRam) && !productName.contains(cleanRam)) {
                            return false;
                        }
                    }
                    // Storage filter
                    if (storage != null && !storage.isBlank() && !"all".equalsIgnoreCase(storage)) {
                        String cleanStorage = storage.toLowerCase().replaceAll("\\s+", "");
                        String offerStorage = offer.getAttributes() != null && offer.getAttributes().getStorage() != null
                                ? offer.getAttributes().getStorage().toLowerCase().replaceAll("\\s+", "")
                                : "";
                        String productName = offer.getProductName() != null ? offer.getProductName().toLowerCase() : "";
                        if (!offerStorage.contains(cleanStorage) && !productName.contains(cleanStorage)) {
                            return false;
                        }
                    }
                    // Delivery filter
                    if (delivery != null && !delivery.isBlank() && !"all".equalsIgnoreCase(delivery)) {
                        String deliv = offer.getDelivery() != null ? offer.getDelivery().toLowerCase() : "";
                        String filter = delivery.toLowerCase().trim();
                        if ("same_day".equals(filter) || "sameday".equals(filter)) {
                            if (!deliv.contains("same day") && !deliv.contains("today")) return false;
                        } else if ("next_day".equals(filter) || "tomorrow".equals(filter)) {
                            if (!deliv.contains("tomorrow") && !deliv.contains("1 day") && !deliv.contains("same day") && !deliv.contains("today")) return false;
                        } else if ("express".equals(filter)) {
                            if (!deliv.contains("express") && !deliv.contains("same day") && !deliv.contains("tomorrow") && !deliv.contains("today")) return false;
                        } else if ("free".equals(filter)) {
                            if (!deliv.contains("free")) return false;
                        } else {
                            if (!deliv.contains(filter)) return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 5. Apply Deterministic Multi-Factor Ranking (Best Value, Price, Rating, Delivery, Discount)
        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(filtered, sortBy);

        // 6. Find Overall Cheapest Price & Merchant
        BigDecimal cheapestPrice = null;
        String cheapestMerchant = null;

        if (!ranked.isEmpty()) {
            NormalizedProductOfferDto cheapest = ranked.stream()
                    .min(Comparator.comparing(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice()))
                    .orElse(ranked.get(0));

            cheapestPrice = cheapest.getEffectivePrice() != null ? cheapest.getEffectivePrice() : cheapest.getPrice();
            cheapestMerchant = cheapest.getMerchant();
        }

        // 7. Generate Structured Deterministic Recommendation & Ranking Explanations
        AiRecommendationDto recommendation = recommendationService.recommendProducts(ranked);
        var rankingSummary = rankingService.getRankingSummary(ranked);

        // 8. Determine Partial Success / Failure status
        String status = "SUCCESS";
        String statusMessage = null;
        if (!failedProviders.isEmpty() && !successfulProviders.isEmpty()) {
            status = "PARTIAL_SUCCESS";
            statusMessage = "Some stores couldn't be reached. Showing available results.";
        } else if (successfulProviders.isEmpty() && !failedProviders.isEmpty()) {
            status = "FAILED";
            statusMessage = "All store providers are currently unavailable. Please try again.";
        }

        return ProductComparisonResponseDto.builder()
                .query(query)
                .totalOffers(ranked.size())
                .cheapestPrice(cheapestPrice)
                .cheapestMerchant(cheapestMerchant)
                .offers(ranked)
                .aiRecommendation(recommendation)
                .rankingSummary(rankingSummary)
                .status(status)
                .statusMessage(statusMessage)
                .successfulProviders(new ArrayList<>(successfulProviders))
                .failedProviders(new ArrayList<>(failedProviders))
                .build();
    }
}
