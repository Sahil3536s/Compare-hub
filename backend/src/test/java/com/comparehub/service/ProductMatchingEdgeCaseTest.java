package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductSimilarityResultDto;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import com.comparehub.service.impl.ProductMatchingServiceImpl;
import com.comparehub.service.impl.ProductSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMatchingEdgeCaseTest {

    private ProductAttributeExtractor extractor;
    private ProductSimilarityService similarityService;
    private ProductMatchingService matchingService;

    @BeforeEach
    void setUp() {
        extractor = new ProductAttributeExtractorImpl();
        similarityService = new ProductSimilarityServiceImpl();
        matchingService = new ProductMatchingServiceImpl(extractor, similarityService);
    }

    @Test
    @DisplayName("Edge Case 1: Samsung S24 128GB MUST NOT match Samsung S24 256GB")
    void testStorageVariantRejection() {
        ProductAttributesDto attr128 = extractor.extractAttributes("Samsung Galaxy S24 5G 128GB Onyx Black", null, null);
        ProductAttributesDto attr256 = extractor.extractAttributes("Samsung Galaxy S24 5G 256GB Onyx Black", null, null);

        ProductSimilarityResultDto similarity = similarityService.calculateSimilarity(
                attr128, attr256,
                "Samsung Galaxy S24 5G 128GB Onyx Black",
                "Samsung Galaxy S24 5G 256GB Onyx Black"
        );

        assertThat(similarity.isCompatible()).isFalse();
        assertThat(similarity.getScore()).isEqualTo(0);
        assertThat(similarity.getMatchQuality()).isEqualTo("SEPARATE_LISTING");
        assertThat(similarity.getExplanation()).containsIgnoringCase("Different storage capacities");

        NormalizedProductOfferDto offer128 = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24 5G 128GB Onyx Black")
                .merchant("Amazon")
                .price(new BigDecimal("74999"))
                .build();

        NormalizedProductOfferDto offer256 = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24 5G 256GB Onyx Black")
                .merchant("Flipkart")
                .price(new BigDecimal("79999"))
                .build();

        List<CanonicalProductGroupDto> groups = matchingService.matchAndGroupOffers(List.of(offer128, offer256));
        assertThat(groups).hasSize(2);
    }

    @Test
    @DisplayName("Edge Case 2: iPhone 15 Pro MUST NOT match iPhone 15 Pro Max")
    void testProVsProMaxVariantRejection() {
        ProductAttributesDto attrPro = extractor.extractAttributes("Apple iPhone 15 Pro (128 GB) - Blue Titanium", null, null);
        ProductAttributesDto attrProMax = extractor.extractAttributes("Apple iPhone 15 Pro Max (256 GB) - Blue Titanium", null, null);

        ProductSimilarityResultDto similarity = similarityService.calculateSimilarity(
                attrPro, attrProMax,
                "Apple iPhone 15 Pro (128 GB) - Blue Titanium",
                "Apple iPhone 15 Pro Max (256 GB) - Blue Titanium"
        );

        assertThat(similarity.isCompatible()).isFalse();
        assertThat(similarity.getScore()).isEqualTo(0);
        assertThat(similarity.getMatchQuality()).isEqualTo("SEPARATE_LISTING");
    }

    @Test
    @DisplayName("Edge Case 3: Samsung Galaxy S24 5G 8GB 256GB Black MUST match SAMSUNG Galaxy S24 (Black, 256 GB)")
    void testCrossMerchantFormatMatching() {
        String titleAmazon = "Samsung Galaxy S24 5G 8GB 256GB Black";
        String titleFlipkart = "SAMSUNG Galaxy S24 (Black, 256 GB)";

        ProductAttributesDto attrAmazon = extractor.extractAttributes(titleAmazon, null, null);
        ProductAttributesDto attrFlipkart = extractor.extractAttributes(titleFlipkart, null, null);

        ProductSimilarityResultDto similarity = similarityService.calculateSimilarity(
                attrAmazon, attrFlipkart, titleAmazon, titleFlipkart
        );

        assertThat(similarity.isCompatible()).isTrue();
        assertThat(similarity.getScore()).isGreaterThanOrEqualTo(75);
        assertThat(similarity.getMatchQuality()).isIn("STRONG_MATCH", "PROBABLE_MATCH");

        NormalizedProductOfferDto offerAmazon = NormalizedProductOfferDto.builder()
                .productName(titleAmazon)
                .merchant("Amazon")
                .price(new BigDecimal("74999"))
                .build();

        NormalizedProductOfferDto offerFlipkart = NormalizedProductOfferDto.builder()
                .productName(titleFlipkart)
                .merchant("Flipkart")
                .price(new BigDecimal("73999"))
                .build();

        List<CanonicalProductGroupDto> groups = matchingService.matchAndGroupOffers(List.of(offerAmazon, offerFlipkart));
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getOffers()).hasSize(2);
        assertThat(groups.get(0).getLowestPrice()).isEqualByComparingTo(new BigDecimal("73999"));
        assertThat(groups.get(0).getCheapestMerchant()).isEqualTo("Flipkart");
    }

    @Test
    @DisplayName("Edge Case 4: Different brands MUST NOT match even with similar model names")
    void testBrandConflictRejection() {
        ProductAttributesDto attrApple = extractor.extractAttributes("Apple Watch Series 9 GPS 45mm", null, null);
        ProductAttributesDto attrSamsung = extractor.extractAttributes("Samsung Galaxy Watch 6 44mm", null, null);

        ProductSimilarityResultDto similarity = similarityService.calculateSimilarity(
                attrApple, attrSamsung,
                "Apple Watch Series 9 GPS 45mm",
                "Samsung Galaxy Watch 6 44mm"
        );

        assertThat(similarity.isCompatible()).isFalse();
        assertThat(similarity.getScore()).isEqualTo(0);
        assertThat(similarity.getExplanation()).containsIgnoringCase("Different brands");
    }

    @Test
    @DisplayName("Edge Case 5: Null attributes must return safe separate listing result")
    void testNullAttributesHandling() {
        ProductSimilarityResultDto similarity = similarityService.calculateSimilarity(
                null, null, "Item A", "Item B"
        );

        assertThat(similarity.isCompatible()).isFalse();
        assertThat(similarity.getScore()).isEqualTo(0);
        assertThat(similarity.getMatchQuality()).isEqualTo("SEPARATE_LISTING");
    }
}
