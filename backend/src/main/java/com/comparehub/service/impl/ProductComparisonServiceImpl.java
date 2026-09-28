package com.comparehub.service.impl;

import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.ProductSearchRequestDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class ProductComparisonServiceImpl implements ProductComparisonService {

    private final ProductSearchOrchestrator orchestrator;

    @Autowired
    public ProductComparisonServiceImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService,
            @Qualifier("providerExecutor") Executor providerExecutor,
            @Autowired(required = false) ProductSearchOrchestrator orchestrator) {
        this.orchestrator = orchestrator != null ? orchestrator
                : new ProductSearchOrchestratorImpl(providers, productMatchingService, normalizationService, rankingService, recommendationService, providerExecutor);
    }

    // Overloaded constructor for tests or default executor
    public ProductComparisonServiceImpl(
            List<ProductProvider> providers,
            ProductMatchingService productMatchingService,
            ProductNormalizationService normalizationService,
            ProductRankingService rankingService,
            ComparisonRecommendationService recommendationService) {
        this(providers, productMatchingService, normalizationService, rankingService, recommendationService, ForkJoinPool.commonPool(), null);
    }

    public ProductComparisonServiceImpl(ProductSearchOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public ProductComparisonResponseDto search(ProductSearchRequestDto request) {
        return orchestrator.search(request);
    }

    @Override
    public List<String> getSearchSuggestions(String prefix) {
        return orchestrator.getSearchSuggestions(prefix);
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

        ProductSearchRequestDto req = ProductSearchRequestDto.builder()
                .query(query)
                .merchant(merchant)
                .brand(brand)
                .category(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStockOnly(inStockOnly)
                .sortBy(sortBy)
                .minRating(minRating)
                .ram(ram)
                .storage(storage)
                .delivery(delivery)
                .page(1)
                .pageSize(50)
                .build();

        return orchestrator.search(req);
    }
}
