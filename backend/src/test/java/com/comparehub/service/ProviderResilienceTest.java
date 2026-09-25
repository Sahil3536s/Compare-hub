package com.comparehub.service;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import com.comparehub.service.impl.ProductComparisonServiceImpl;
import com.comparehub.service.impl.ProductMatchingServiceImpl;
import com.comparehub.service.impl.ProductSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderResilienceTest {

    @Mock
    private ProductProvider amazonProvider;

    @Mock
    private ProductProvider flipkartProvider;

    @Mock
    private ProductProvider cromaProvider;

    @Mock
    private ProductNormalizationService normalizationService;

    @Mock
    private ProductRankingService rankingService;

    @Mock
    private ComparisonRecommendationService recommendationService;

    private ProductComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        ProductAttributeExtractor extractor = new ProductAttributeExtractorImpl();
        ProductSimilarityService similarityService = new ProductSimilarityServiceImpl();
        ProductMatchingService matchingService = new ProductMatchingServiceImpl(extractor, similarityService);

        comparisonService = new ProductComparisonServiceImpl(
                List.of(amazonProvider, flipkartProvider, cromaProvider),
                matchingService,
                normalizationService,
                rankingService,
                recommendationService
        );

        lenient().when(amazonProvider.getProviderName()).thenReturn("Amazon");
        lenient().when(flipkartProvider.getProviderName()).thenReturn("Flipkart");
        lenient().when(cromaProvider.getProviderName()).thenReturn("Croma");
    }

    @Test
    @DisplayName("Should survive 2 out of 3 provider failures and return results from the 1 healthy provider")
    void testPartialProviderOutage() {
        when(amazonProvider.searchProducts("laptop")).thenThrow(new RuntimeException("Connection timed out to Amazon"));
        when(cromaProvider.searchProducts("laptop")).thenThrow(new IllegalStateException("Croma downstream 503"));

        NormalizedProductOfferDto flipkartOffer = NormalizedProductOfferDto.builder()
                .productName("MacBook Air M3")
                .merchant("Flipkart")
                .price(new BigDecimal("104900"))
                .inStock(true)
                .build();

        when(flipkartProvider.searchProducts("laptop")).thenReturn(List.of(flipkartOffer));
        when(normalizationService.normalizeOffers(any())).thenReturn(List.of(flipkartOffer));
        when(rankingService.rankAndMarkCheapest(any(), any())).thenReturn(List.of(flipkartOffer));
        when(rankingService.getRankingSummary(any())).thenReturn(RankingSummaryDto.builder().cheapest("Flipkart").build());
        when(recommendationService.recommendProducts(any())).thenReturn(AiRecommendationDto.builder().bestOverall("Flipkart").build());

        ProductComparisonResponseDto result = assertDoesNotThrow(() ->
                comparisonService.compareProducts("laptop", "all", "all", "all", null, null, false, "best")
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalOffers()).isEqualTo(1);
        assertThat(result.getOffers().get(0).getMerchant()).isEqualTo("Flipkart");
        assertThat(result.getCheapestMerchant()).isEqualTo("Flipkart");
    }

    @Test
    @DisplayName("Should return safe empty response when all providers fail simultaneously without throwing 500")
    void testTotalProviderOutage() {
        when(amazonProvider.searchProducts("headphones")).thenThrow(new RuntimeException("Amazon down"));
        when(flipkartProvider.searchProducts("headphones")).thenThrow(new RuntimeException("Flipkart down"));
        when(cromaProvider.searchProducts("headphones")).thenThrow(new RuntimeException("Croma down"));

        when(normalizationService.normalizeOffers(any())).thenReturn(List.of());
        when(rankingService.rankAndMarkCheapest(any(), any())).thenReturn(List.of());
        when(rankingService.getRankingSummary(any())).thenReturn(RankingSummaryDto.builder().cheapest("N/A").build());
        when(recommendationService.recommendProducts(any())).thenReturn(AiRecommendationDto.builder().bestOverall("N/A").build());

        ProductComparisonResponseDto result = assertDoesNotThrow(() ->
                comparisonService.compareProducts("headphones", "all", "all", "all", null, null, false, "best")
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalOffers()).isEqualTo(0);
        assertThat(result.getOffers()).isEmpty();
        assertThat(result.getCheapestPrice()).isNull();
        assertThat(result.getCheapestMerchant()).isNull();
    }

    @Test
    @DisplayName("Should gracefully handle providers returning null instead of empty list")
    void testProviderReturningNull() {
        when(amazonProvider.searchProducts("mouse")).thenReturn(null);
        when(flipkartProvider.searchProducts("mouse")).thenReturn(null);
        when(cromaProvider.searchProducts("mouse")).thenReturn(null);

        when(normalizationService.normalizeOffers(any())).thenReturn(List.of());
        when(rankingService.rankAndMarkCheapest(any(), any())).thenReturn(List.of());
        when(rankingService.getRankingSummary(any())).thenReturn(RankingSummaryDto.builder().cheapest("N/A").build());
        when(recommendationService.recommendProducts(any())).thenReturn(AiRecommendationDto.builder().bestOverall("N/A").build());

        ProductComparisonResponseDto result = assertDoesNotThrow(() ->
                comparisonService.compareProducts("mouse", "all", "all", "all", null, null, false, "best")
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalOffers()).isEqualTo(0);
    }
}
