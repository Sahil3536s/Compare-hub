package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.ProductSearchRequestDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.provider.impl.AmazonProductProvider;
import com.comparehub.provider.impl.CromaProductProvider;
import com.comparehub.provider.impl.FlipkartProductProvider;
import com.comparehub.provider.impl.OpenProductDataProvider;
import com.comparehub.service.impl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;

class UniversalProductSearchAcceptanceTest {

    private ProductSearchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ProductProvider openCommerce = new OpenProductDataProvider(RestClient.create(), new ObjectMapper());
        ProductProvider amazon = new AmazonProductProvider();
        ProductProvider flipkart = new FlipkartProductProvider();
        ProductProvider croma = new CromaProductProvider();

        ProductAttributeExtractor extractor = new ProductAttributeExtractorImpl();
        ProductSimilarityService similarityService = new ProductSimilarityServiceImpl();
        ProductMatchingService matchingService = new ProductMatchingServiceImpl(extractor, similarityService);

        ProductNormalizationService normalizationService = org.mockito.Mockito.mock(ProductNormalizationService.class);
        org.mockito.Mockito.lenient().when(normalizationService.normalizeOffer(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(normalizationService.normalizeOffers(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductRankingService rankingService = new ProductRankingServiceImpl();
        ComparisonRecommendationService recommendationService = new ComparisonRecommendationServiceImpl();

        orchestrator = new ProductSearchOrchestratorImpl(
                List.of(openCommerce, amazon, flipkart, croma),
                matchingService,
                normalizationService,
                rankingService,
                recommendationService,
                ForkJoinPool.commonPool()
        );
    }

    @ParameterizedTest(name = "Search query: {0}")
    @ValueSource(strings = {
            "wireless mouse",
            "nike running shoes",
            "men black shirt",
            "women handbag",
            "office chair",
            "study table",
            "coffee maker",
            "water bottle",
            "refrigerator",
            "washing machine",
            "smartwatch",
            "perfume",
            "sunglasses"
    })
    @DisplayName("Universal search works dynamically across normal retail categories")
    void shouldSearchArbitraryRetailProductQueries(String query) {
        ProductSearchRequestDto request = ProductSearchRequestDto.builder()
                .query(query)
                .page(1)
                .pageSize(10)
                .sortBy("best")
                .build();

        ProductComparisonResponseDto response = orchestrator.search(request);

        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getStatus()).isIn("SUCCESS", "PARTIAL_SUCCESS");
        assertThat(response.getOffers()).isNotNull();

        // Verify dynamic facets are generated without throwing exceptions
        assertThat(response.getDynamicFilters()).isNotNull();
        assertThat(response.getAvailableBrands()).isNotNull();
        assertThat(response.getAvailableCategories()).isNotNull();
        assertThat(response.getAvailableMerchants()).isNotNull();

        // Verify pagination metadata
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalPages()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Search suggestions return dynamic matches without hardcoding")
    void shouldReturnSuggestionsDynamically() {
        List<String> suggestions = orchestrator.getSearchSuggestions("wire");
        assertThat(suggestions).isNotNull();
        for (String suggestion : suggestions) {
            assertThat(suggestion.toLowerCase()).contains("wire");
        }
    }

    @Test
    @DisplayName("Pagination slices results accurately with hasMore flag")
    void shouldPaginateResultsAccurately() {
        ProductSearchRequestDto page1Req = ProductSearchRequestDto.builder()
                .query("laptop")
                .page(1)
                .pageSize(2)
                .build();

        ProductComparisonResponseDto page1 = orchestrator.search(page1Req);
        assertThat(page1.getOffers().size()).isLessThanOrEqualTo(2);

        if (page1.getTotalOffers() > 2) {
            assertThat(page1.getHasMore()).isTrue();

            ProductSearchRequestDto page2Req = ProductSearchRequestDto.builder()
                    .query("laptop")
                    .page(2)
                    .pageSize(2)
                    .build();

            ProductComparisonResponseDto page2 = orchestrator.search(page2Req);
            assertThat(page2.getOffers()).isNotNull();
            assertThat(page2.getPage()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Accessory downranking places main product before phone cases")
    void shouldDownrankAccessories() {
        ProductSearchRequestDto req = ProductSearchRequestDto.builder()
                .query("iPhone 15")
                .page(1)
                .pageSize(20)
                .sortBy("best")
                .build();

        ProductComparisonResponseDto response = orchestrator.search(req);
        List<NormalizedProductOfferDto> offers = response.getOffers();

        if (offers.size() >= 2) {
            NormalizedProductOfferDto first = offers.get(0);
            boolean isTopAccessory = first.getTitle().toLowerCase().contains("case")
                    || first.getTitle().toLowerCase().contains("cover");
            assertThat(isTopAccessory).isFalse();
        }
    }

    @Test
    @DisplayName("Dynamic facet filtering by merchant filters correctly")
    void shouldFilterByMerchant() {
        ProductSearchRequestDto req = ProductSearchRequestDto.builder()
                .query("mouse")
                .merchant("Amazon")
                .page(1)
                .pageSize(10)
                .build();

        ProductComparisonResponseDto response = orchestrator.search(req);
        for (NormalizedProductOfferDto offer : response.getOffers()) {
            assertThat(offer.getMerchant()).isEqualToIgnoringCase("Amazon");
        }
    }
}
