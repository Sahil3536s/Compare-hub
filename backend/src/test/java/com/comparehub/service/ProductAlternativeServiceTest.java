package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAlternativeServiceTest {

    @Mock
    private ProductProvider productProvider;

    private ProductAlternativeService alternativeService;

    @BeforeEach
    void setUp() {
        ProductAttributeExtractor extractor = new ProductAttributeExtractorImpl();
        ProductSimilarityService similarityService = new ProductSimilarityServiceImpl();
        ProductMatchingService matchingService = new ProductMatchingServiceImpl(extractor, similarityService);
        ProductFeatureComparisonService featureService = new ProductFeatureComparisonServiceImpl();

        alternativeService = new ProductAlternativeServiceImpl(
                List.of(productProvider), matchingService, featureService);
    }

    @Test
    @DisplayName("Should categorize alternatives into Cheaper, Value, Feature Upgrade, and Premium tiers")
    void testFindAlternatives() {
        NormalizedProductOfferDto base = NormalizedProductOfferDto.builder()
                .productName("Apple iPhone 15 (128GB) - Black")
                .price(new BigDecimal("79900"))
                .category("Smartphones")
                .brand("Apple")
                .rating(4.7)
                .build();

        List<NormalizedProductOfferDto> catalog = List.of(
                NormalizedProductOfferDto.builder()
                        .productName("OnePlus 12 5G (16GB RAM, 512GB Storage)")
                        .price(new BigDecimal("64999"))
                        .category("Smartphones")
                        .brand("OnePlus")
                        .rating(4.8)
                        .inStock(true)
                        .build(),
                NormalizedProductOfferDto.builder()
                        .productName("Samsung Galaxy S24 5G (8GB RAM, 256GB)")
                        .price(new BigDecimal("74999"))
                        .category("Smartphones")
                        .brand("Samsung")
                        .rating(4.7)
                        .inStock(true)
                        .build(),
                NormalizedProductOfferDto.builder()
                        .productName("Apple iPhone 15 Pro Max (256GB) - Titanium")
                        .price(new BigDecimal("134900"))
                        .category("Smartphones")
                        .brand("Apple")
                        .rating(4.9)
                        .inStock(true)
                        .build()
        );

        when(productProvider.searchProducts(anyString())).thenReturn(catalog);

        ProductAlternativesResponseDto response = alternativeService.getAlternatives(
                "Apple iPhone 15 (128GB)", new BigDecimal("79900"), "Smartphones", "Apple", 4);

        assertThat(response).isNotNull();
        assertThat(response.getAlternatives()).isNotEmpty();
        assertThat(response.getTotalAlternatives()).isGreaterThanOrEqualTo(2);

        ProductAlternativeDto onePlusAlt = response.getAlternatives().stream()
                .filter(a -> a.getProductName().contains("OnePlus"))
                .findFirst()
                .orElse(null);

        assertThat(onePlusAlt).isNotNull();
        assertThat(onePlusAlt.getPriceDifference()).isNegative();
        assertThat(onePlusAlt.getCategoryType()).isIn(AlternativeCategoryType.CHEAPER_ALTERNATIVE, AlternativeCategoryType.BETTER_VALUE);
        assertThat(onePlusAlt.getHighlights()).isNotEmpty();
    }
}
