package com.comparehub.service;

import com.comparehub.dto.AlternativeCategoryType;
import com.comparehub.dto.FeatureComparisonDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.service.impl.ProductFeatureComparisonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFeatureComparisonServiceTest {

    private ProductFeatureComparisonService featureComparisonService;

    @BeforeEach
    void setUp() {
        featureComparisonService = new ProductFeatureComparisonServiceImpl();
    }

    @Test
    @DisplayName("Should correctly compare features and price differences between two products")
    void testCompareFeatures() {
        NormalizedProductOfferDto base = NormalizedProductOfferDto.builder()
                .productName("iPhone 17 (128GB)")
                .price(new BigDecimal("70000"))
                .rating(4.6)
                .attributes(ProductAttributesDto.builder()
                        .brand("Apple")
                        .model("iPhone 17")
                        .ram("8GB")
                        .storage("128GB")
                        .screenSize("6.1 inch")
                        .build())
                .build();

        NormalizedProductOfferDto alternative = NormalizedProductOfferDto.builder()
                .productName("OnePlus 13 (256GB)")
                .price(new BigDecimal("62000"))
                .rating(4.8)
                .attributes(ProductAttributesDto.builder()
                        .brand("OnePlus")
                        .model("OnePlus 13")
                        .ram("12GB")
                        .storage("256GB")
                        .screenSize("6.7 inch")
                        .build())
                .build();

        FeatureComparisonDto comp = featureComparisonService.compareFeatures(base, alternative);

        assertThat(comp).isNotNull();
        assertThat(comp.getPriceDiff()).isEqualByComparingTo(new BigDecimal("-8000"));
        assertThat(comp.getIsCheaper()).isTrue();
        assertThat(comp.getPriceDiffPercent()).isLessThan(0.0);
        assertThat(comp.getRatingDiff()).isEqualTo(0.2);
        assertThat(comp.getRamComparison()).contains("+4GB RAM");
        assertThat(comp.getStorageComparison()).contains("+128GB Storage");
        assertThat(comp.getScreenSizeComparison()).contains("Larger Display");
        assertThat(comp.getBetterSpecsCount()).isGreaterThanOrEqualTo(3);

        List<String> highlights = featureComparisonService.generateHighlights(
                base, alternative, comp, AlternativeCategoryType.CHEAPER_ALTERNATIVE);

        assertThat(highlights).isNotEmpty();
        assertThat(highlights.stream().anyMatch(h -> h.contains("cheaper"))).isTrue();
        assertThat(highlights.stream().anyMatch(h -> h.contains("RAM"))).isTrue();
        assertThat(highlights.stream().anyMatch(h -> h.contains("rating"))).isTrue();
    }
}
