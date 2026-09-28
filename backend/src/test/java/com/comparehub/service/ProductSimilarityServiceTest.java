package com.comparehub.service;

import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductSimilarityResultDto;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import com.comparehub.service.impl.ProductSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSimilarityServiceTest {

    private ProductAttributeExtractor extractor;
    private ProductSimilarityService similarityService;

    @BeforeEach
    void setUp() {
        extractor = new ProductAttributeExtractorImpl();
        similarityService = new ProductSimilarityServiceImpl();
    }

    @Test
    @DisplayName("Should detect STRONG_MATCH (>=90) between differently phrased listings of the same product")
    void testStrongMatchForIdenticalProduct() {
        String titleA = "Samsung Galaxy S26 5G 12GB 256GB Black";
        String titleB = "SAMSUNG S26 (Black, 256 GB)";

        ProductAttributesDto attrA = extractor.extractAttributes(titleA, null, null);
        ProductAttributesDto attrB = extractor.extractAttributes(titleB, null, null);

        ProductSimilarityResultDto result = similarityService.calculateSimilarity(attrA, attrB, titleA, titleB);

        assertThat(result.isCompatible()).isTrue();
        assertThat(result.getScore()).isGreaterThanOrEqualTo(85);
        assertThat(result.getMatchQuality()).isIn("STRONG_MATCH", "PROBABLE_MATCH");
    }

    @Test
    @DisplayName("Should REJECT matching (score 0) when storage capacities conflict (128GB vs 256GB)")
    void testRejectStorageMismatch() {
        ProductAttributesDto attrA = ProductAttributesDto.builder()
                .brand("Samsung")
                .model("Galaxy S26")
                .storage("128GB")
                .build();

        ProductAttributesDto attrB = ProductAttributesDto.builder()
                .brand("Samsung")
                .model("Galaxy S26")
                .storage("256GB")
                .build();

        ProductSimilarityResultDto result = similarityService.calculateSimilarity(
                attrA, attrB, "Samsung S26 128GB", "Samsung S26 256GB");

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getMatchQuality()).isEqualTo("SEPARATE_LISTING");
        assertThat(result.getExplanation()).containsIgnoringCase("Different storage capacities");
    }

    @Test
    @DisplayName("Should REJECT matching (score 0) when variants conflict (iPhone 15 vs iPhone 15 Pro Max)")
    void testRejectVariantMismatch() {
        ProductAttributesDto attrA = ProductAttributesDto.builder()
                .brand("Apple")
                .model("iPhone 15")
                .storage("256GB")
                .build();

        ProductAttributesDto attrB = ProductAttributesDto.builder()
                .brand("Apple")
                .model("iPhone 15")
                .variant("Pro Max")
                .storage("256GB")
                .build();

        ProductSimilarityResultDto result = similarityService.calculateSimilarity(
                attrA, attrB, "Apple iPhone 15 256GB", "Apple iPhone 15 Pro Max 256GB");

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getMatchQuality()).isEqualTo("SEPARATE_LISTING");
    }

    @Test
    @DisplayName("Should REJECT matching (score 0) when brands differ")
    void testRejectBrandMismatch() {
        ProductAttributesDto attrA = ProductAttributesDto.builder()
                .brand("Apple")
                .model("iPhone 15")
                .build();

        ProductAttributesDto attrB = ProductAttributesDto.builder()
                .brand("Samsung")
                .model("Galaxy S24")
                .build();

        ProductSimilarityResultDto result = similarityService.calculateSimilarity(
                attrA, attrB, "Apple iPhone 15", "Samsung Galaxy S24");

        assertThat(result.isCompatible()).isFalse();
        assertThat(result.getScore()).isEqualTo(0);
    }
}
