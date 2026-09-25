package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductMatchResultDto;
import com.comparehub.service.impl.ProductAttributeExtractorImpl;
import com.comparehub.service.impl.ProductMatchingServiceImpl;
import com.comparehub.service.impl.ProductSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMatchingServiceTest {

    private ProductMatchingService matchingService;

    @BeforeEach
    void setUp() {
        ProductAttributeExtractor extractor = new ProductAttributeExtractorImpl();
        ProductSimilarityService similarityService = new ProductSimilarityServiceImpl();
        matchingService = new ProductMatchingServiceImpl(extractor, similarityService);
    }

    @Test
    @DisplayName("1. Explainable Match: Provider A and Provider B listings of Samsung Galaxy S24 match with high score and reasons")
    void testExplainableMatchSamsungS24() {
        NormalizedProductOfferDto offerA = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 5G (Onyx Black, 256 GB) (8 GB RAM)")
                .merchant("Amazon")
                .price(new BigDecimal("59999"))
                .brand("SAMSUNG")
                .storage("256 GB")
                .ram("8 GB RAM")
                .build();

        NormalizedProductOfferDto offerB = NormalizedProductOfferDto.builder()
                .title("SAMSUNG S24 (Black, 256GB)")
                .merchant("Flipkart")
                .price(new BigDecimal("58499"))
                .brand("samsung")
                .storage("256GB")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offerA, offerB);

        assertThat(matchResult.isMatched()).isTrue();
        assertThat(matchResult.getScore()).isGreaterThanOrEqualTo(0.85);
        assertThat(matchResult.getReasons()).contains("Same brand", "Same model", "Same storage");
    }

    @Test
    @DisplayName("2. Explainable Rejection: Storage mismatch 128GB vs 256GB returns matched=false with explainable conflict reason")
    void testExplainableRejectionStorageMismatch() {
        NormalizedProductOfferDto offer128 = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 5G (128 GB)")
                .merchant("Amazon")
                .price(new BigDecimal("54999"))
                .storage("128GB")
                .brand("Samsung")
                .build();

        NormalizedProductOfferDto offer256 = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 5G (256 GB)")
                .merchant("Flipkart")
                .price(new BigDecimal("59999"))
                .storage("256GB")
                .brand("Samsung")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offer128, offer256);

        assertThat(matchResult.isMatched()).isFalse();
        assertThat(matchResult.getScore()).isGreaterThan(0.50); // Model/brand similarity is still recognized
        assertThat(matchResult.getReasons()).anyMatch(r -> r.contains("Storage mismatch: 128GB vs 256GB"));
    }

    @Test
    @DisplayName("3. Strict Variant Safety: 8GB RAM vs 12GB RAM MUST NOT match")
    void testRamMismatchRejection() {
        NormalizedProductOfferDto offer8GB = NormalizedProductOfferDto.builder()
                .title("OnePlus 12R (8GB RAM, 128GB Storage)")
                .merchant("Amazon")
                .price(new BigDecimal("39999"))
                .ram("8GB")
                .storage("128GB")
                .brand("OnePlus")
                .build();

        NormalizedProductOfferDto offer12GB = NormalizedProductOfferDto.builder()
                .title("OnePlus 12R (12GB RAM, 128GB Storage)")
                .merchant("Flipkart")
                .price(new BigDecimal("42999"))
                .ram("12GB")
                .storage("128GB")
                .brand("OnePlus")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offer8GB, offer12GB);

        assertThat(matchResult.isMatched()).isFalse();
        assertThat(matchResult.getReasons()).anyMatch(r -> r.contains("RAM mismatch: 8GB vs 12GB"));
    }

    @Test
    @DisplayName("4. Strict Variant Safety: S24 vs S24+ MUST NOT match")
    void testS24VsS24PlusRejection() {
        NormalizedProductOfferDto offerS24 = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 (256 GB)")
                .merchant("Amazon")
                .price(new BigDecimal("59999"))
                .model("Galaxy S24")
                .storage("256GB")
                .brand("Samsung")
                .build();

        NormalizedProductOfferDto offerS24Plus = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24+ (256 GB)")
                .merchant("Flipkart")
                .price(new BigDecimal("79999"))
                .model("Galaxy S24+")
                .variant("Plus")
                .storage("256GB")
                .brand("Samsung")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offerS24, offerS24Plus);

        assertThat(matchResult.isMatched()).isFalse();
        assertThat(matchResult.getReasons()).anyMatch(r -> r.contains("Model mismatch: S24 vs S24+") || r.contains("Variant mismatch"));
    }

    @Test
    @DisplayName("5. Strict Variant Safety: iPhone 16 vs iPhone 16 Pro MUST NOT match")
    void testIphone16VsIphone16ProRejection() {
        NormalizedProductOfferDto offerBase = NormalizedProductOfferDto.builder()
                .title("Apple iPhone 16 (128 GB) - Black")
                .merchant("Amazon")
                .price(new BigDecimal("79900"))
                .brand("Apple")
                .storage("128GB")
                .build();

        NormalizedProductOfferDto offerPro = NormalizedProductOfferDto.builder()
                .title("Apple iPhone 16 Pro (128 GB) - Natural Titanium")
                .merchant("Flipkart")
                .price(new BigDecimal("119900"))
                .brand("Apple")
                .storage("128GB")
                .variant("Pro")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offerBase, offerPro);

        assertThat(matchResult.isMatched()).isFalse();
        assertThat(matchResult.getReasons()).anyMatch(r -> r.contains("Variant mismatch"));
    }

    @Test
    @DisplayName("6. Strict Variant Safety: Different model numbers MUST NOT match")
    void testDifferentModelNumbersRejection() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 SM-S921B")
                .merchant("Amazon")
                .price(new BigDecimal("59999"))
                .modelNumber("SM-S921B")
                .brand("Samsung")
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 SM-S926B")
                .merchant("Flipkart")
                .price(new BigDecimal("59999"))
                .modelNumber("SM-S926B")
                .brand("Samsung")
                .build();

        ProductMatchResultDto matchResult = matchingService.evaluateMatch(offer1, offer2);

        assertThat(matchResult.isMatched()).isFalse();
        assertThat(matchResult.getReasons()).anyMatch(r -> r.contains("Model number mismatch"));
    }

    @Test
    @DisplayName("7. Canonical grouping: Group identical product listings into one cluster")
    void testGroupIdenticalProductListings() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S26 5G 12GB 256GB Black")
                .merchant("Amazon")
                .price(new BigDecimal("74999"))
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .productName("SAMSUNG S26 (Black, 256 GB)")
                .merchant("Flipkart")
                .price(new BigDecimal("72999"))
                .build();

        NormalizedProductOfferDto offer3 = NormalizedProductOfferDto.builder()
                .productName("Galaxy S26 12/256GB 5G")
                .merchant("Croma")
                .price(new BigDecimal("75999"))
                .build();

        List<CanonicalProductGroupDto> groups = matchingService.matchAndGroupOffers(
                List.of(offer1, offer2, offer3));

        assertThat(groups).hasSize(1);
        CanonicalProductGroupDto group = groups.get(0);
        assertThat(group.getOffers()).hasSize(3);
        assertThat(group.getLowestPrice()).isEqualByComparingTo(new BigDecimal("72999"));
        assertThat(group.getCheapestMerchant()).isEqualTo("Flipkart");
        assertThat(group.getPriceSpread()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(offer2.getIsCheapest()).isTrue();
    }

    @Test
    @DisplayName("8. Split 128GB and 256GB into 2 distinct canonical groups")
    void testSplitDifferentVariantsIntoSeparateGroups() {
        NormalizedProductOfferDto offer128GB = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S26 5G 128GB")
                .merchant("Amazon")
                .price(new BigDecimal("64999"))
                .build();

        NormalizedProductOfferDto offer256GB = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S26 5G 256GB")
                .merchant("Flipkart")
                .price(new BigDecimal("74999"))
                .build();

        List<CanonicalProductGroupDto> groups = matchingService.matchAndGroupOffers(
                List.of(offer128GB, offer256GB));

        assertThat(groups).hasSize(2);
    }
}
