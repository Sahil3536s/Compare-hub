package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import com.comparehub.service.impl.ProductComparisonServiceImpl;
import com.comparehub.service.impl.ProductMatchingServiceImpl;
import com.comparehub.service.impl.ProductSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductComparisonServiceTest {

    @Mock
    private ProductProvider amazonProvider;

    @Mock
    private ProductProvider flipkartProvider;

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
                List.of(amazonProvider, flipkartProvider),
                matchingService,
                normalizationService,
                rankingService,
                recommendationService
        );
    }

    @Test
    void shouldAggregateOffersFromMultipleProviders() {
        NormalizedProductOfferDto amazonOffer = NormalizedProductOfferDto.builder()
                .productName("iPhone 15")
                .merchant("Amazon")
                .price(new BigDecimal("79900.00"))
                .inStock(true)
                .build();

        NormalizedProductOfferDto flipkartOffer = NormalizedProductOfferDto.builder()
                .productName("iPhone 15")
                .merchant("Flipkart")
                .price(new BigDecimal("78900.00"))
                .inStock(true)
                .isCheapest(true)
                .build();

        when(amazonProvider.searchProducts("iphone")).thenReturn(List.of(amazonOffer));
        when(flipkartProvider.searchProducts("iphone")).thenReturn(List.of(flipkartOffer));
        when(normalizationService.normalizeOffers(any())).thenReturn(List.of(amazonOffer, flipkartOffer));
        when(rankingService.rankAndMarkCheapest(any(), any())).thenReturn(List.of(flipkartOffer, amazonOffer));
        when(rankingService.getRankingSummary(any())).thenReturn(RankingSummaryDto.builder().cheapest("Flipkart").build());

        ProductComparisonResponseDto result = comparisonService.compareProducts(
                "iphone", "all", "all", "all", null, null, false, "price_asc");

        assertNotNull(result);
        assertEquals(2, result.getTotalOffers());
        assertEquals("Flipkart", result.getCheapestMerchant());
        assertEquals(new BigDecimal("78900.00"), result.getCheapestPrice());
        assertEquals(2, result.getOffers().size());
    }

    @Test
    void shouldContinueComparisonWhenOneProviderFails() {
        when(amazonProvider.getProviderName()).thenReturn("Amazon");
        when(amazonProvider.searchProducts("iphone")).thenThrow(new RuntimeException("Amazon API connection timeout"));

        NormalizedProductOfferDto flipkartOffer = NormalizedProductOfferDto.builder()
                .productName("iPhone 15")
                .merchant("Flipkart")
                .price(new BigDecimal("78900.00"))
                .inStock(true)
                .isCheapest(true)
                .build();

        when(flipkartProvider.searchProducts("iphone")).thenReturn(List.of(flipkartOffer));
        when(normalizationService.normalizeOffers(any())).thenReturn(List.of(flipkartOffer));
        when(rankingService.rankAndMarkCheapest(any(), any())).thenReturn(List.of(flipkartOffer));
        when(rankingService.getRankingSummary(any())).thenReturn(RankingSummaryDto.builder().cheapest("Flipkart").build());

        ProductComparisonResponseDto result = assertDoesNotThrow(() ->
                comparisonService.compareProducts("iphone", "all", "all", "all", null, null, false, "best")
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalOffers());
        assertEquals("Flipkart", result.getOffers().get(0).getMerchant());
    }
}
