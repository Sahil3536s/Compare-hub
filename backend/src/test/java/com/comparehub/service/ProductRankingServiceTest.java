package com.comparehub.service;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.impl.ProductRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductRankingServiceTest {

    private ProductRankingService rankingService;

    @BeforeEach
    void setUp() {
        RankingConfigProperties properties = new RankingConfigProperties();
        rankingService = new ProductRankingServiceImpl(properties);
    }

    @Test
    void shouldMarkCheapestOfferAndSortAscending() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .productName("iPhone 15 Pro")
                .merchant("Amazon")
                .price(new BigDecimal("124900.00"))
                .inStock(true)
                .rating(4.8)
                .delivery("Tomorrow")
                .discountPercent(10)
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .productName("iPhone 15 Pro")
                .merchant("Flipkart")
                .price(new BigDecimal("121990.00"))
                .inStock(true)
                .rating(4.5)
                .delivery("2-3 days")
                .discountPercent(12)
                .build();

        NormalizedProductOfferDto offer3 = NormalizedProductOfferDto.builder()
                .productName("iPhone 15 Pro")
                .merchant("Croma")
                .price(new BigDecimal("129990.00"))
                .inStock(true)
                .rating(4.6)
                .delivery("In 5 days")
                .discountPercent(5)
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(offer1, offer2, offer3), "price_asc");

        assertEquals(3, ranked.size());
        assertEquals("Flipkart", ranked.get(0).getMerchant());
        assertTrue(ranked.get(0).getIsCheapest());
        assertFalse(ranked.get(1).getIsCheapest());
        assertFalse(ranked.get(2).getIsCheapest());

        RankingSummaryDto summary = rankingService.getRankingSummary(ranked);
        assertNotNull(summary);
        assertTrue(summary.getCheapest().contains("Flipkart"));
        assertNotNull(summary.getBestValue());
        assertTrue(summary.getExplanation().contains("deterministic weights"));
    }

    @Test
    void shouldCalculateDeterministicBestValueWithConfiguredWeights() {
        NormalizedProductOfferDto amazon = NormalizedProductOfferDto.builder()
                .productName("Sony WH-1000XM5")
                .merchant("Amazon")
                .price(new BigDecimal("70500.00"))
                .inStock(true)
                .rating(4.9)
                .delivery("Tomorrow")
                .discountPercent(18)
                .build();

        NormalizedProductOfferDto flipkart = NormalizedProductOfferDto.builder()
                .productName("Sony WH-1000XM5")
                .merchant("Flipkart")
                .price(new BigDecimal("69990.00"))
                .inStock(true)
                .rating(3.9)
                .delivery("In 5 days")
                .discountPercent(5)
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(amazon, flipkart), "best");

        assertEquals(2, ranked.size());
        assertEquals("Amazon", ranked.get(0).getMerchant());
        assertTrue(ranked.get(0).getIsBestValue());
        assertTrue(ranked.get(0).getRankingScore() > ranked.get(1).getRankingScore());
    }

    @Test
    @DisplayName("Should sort by effective price considering post-discount final price")
    void shouldSortByEffectivePrice() {
        NormalizedProductOfferDto amazon = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24")
                .merchant("Amazon")
                .price(new BigDecimal("75000.00"))
                .effectivePrice(new BigDecimal("73500.00")) // ₹1,500 bank discount
                .build();

        NormalizedProductOfferDto flipkart = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24")
                .merchant("Flipkart")
                .price(new BigDecimal("74000.00")) // lower base price
                .effectivePrice(new BigDecimal("74003.00")) // ₹3 platform fee
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(amazon, flipkart), "effective_price_asc");

        assertEquals(2, ranked.size());
        // Amazon has lower effective price (73500 < 74003)
        assertEquals("Amazon", ranked.get(0).getMerchant());
    }
}
