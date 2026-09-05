package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
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
    @DisplayName("Should group 3 differently named listings of the same product into 1 canonical group")
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
    @DisplayName("Should split 128GB and 256GB into 2 distinct canonical groups")
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
