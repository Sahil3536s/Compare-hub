package com.comparehub.service;

import com.comparehub.config.ComparisonRankingProperties;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.RankResultDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.impl.ProductRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductRankingServiceTest {

    private ProductRankingService rankingService;
    private ComparisonRankingProperties rankingConfig;

    @BeforeEach
    void setUp() {
        rankingConfig = new ComparisonRankingProperties();
        rankingConfig.setPriceWeight(0.45);
        rankingConfig.setRatingWeight(0.25);
        rankingConfig.setDeliveryWeight(0.15);
        rankingConfig.setDiscountWeight(0.10);
        rankingConfig.setAvailabilityWeight(0.05);
        rankingConfig.validateWeights();
        rankingService = new ProductRankingServiceImpl(rankingConfig);
    }

    @Test
    @DisplayName("1. CHEAPEST mode: Selects lowest reliable effective cost and falls back to displayed price")
    void testCheapestModeWithEffectiveCostAndFallback() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .title("Logitech Mouse")
                .merchant("Amazon")
                .price(new BigDecimal("1299"))
                .effectiveCost(new BigDecimal("1349")) // ₹1,299 + ₹50 delivery
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .title("Logitech Mouse")
                .merchant("Flipkart")
                .price(new BigDecimal("1320"))
                .effectiveCost(new BigDecimal("1320")) // ₹1,320 with FREE delivery
                .build();

        NormalizedProductOfferDto offer3 = NormalizedProductOfferDto.builder()
                .title("Logitech Mouse")
                .merchant("Croma")
                .price(new BigDecimal("1399"))
                .effectiveCost(null) // Unknown delivery fee, falls back to displayed price
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(offer1, offer2, offer3), "cheapest");

        // Flipkart has lower effective cost (1320 < 1349) even though Amazon had lower base listed price (1299)
        assertThat(ranked.get(0).getMerchant()).isEqualTo("Flipkart");
        assertThat(ranked.get(0).getIsCheapest()).isTrue();
        assertThat(ranked.get(0).getRankingLabel()).isEqualTo("CHEAPEST");
        assertThat(ranked.get(0).getRecommendationReason()).contains("Lowest known effective cost among 3 available offers");
        assertThat(ranked.get(0).getWhyThisOption()).contains("Lowest effective price");
    }

    @Test
    @DisplayName("2. HIGHEST_RATED mode: Review count damping ensures 4.8 with 20k reviews beats 5.0 with 1 review")
    void testHighestRatedModeReviewCountDamping() {
        NormalizedProductOfferDto highRatingLowReviews = NormalizedProductOfferDto.builder()
                .title("Sony Headphones")
                .merchant("UnknownStore")
                .price(new BigDecimal("19999"))
                .rating(5.0)
                .reviewCount(1) // Only 1 review!
                .build();

        NormalizedProductOfferDto reliableRatingHighReviews = NormalizedProductOfferDto.builder()
                .title("Sony Headphones")
                .merchant("Amazon")
                .price(new BigDecimal("19999"))
                .rating(4.8)
                .reviewCount(20000) // 20,000 verified reviews!
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(highRatingLowReviews, reliableRatingHighReviews), "highest_rated");

        // 4.8 with 20,000 reviews MUST rank #1 over 5.0 with 1 review
        assertThat(ranked.get(0).getMerchant()).isEqualTo("Amazon");
        assertThat(ranked.get(0).getIsHighestRated()).isTrue();
        assertThat(ranked.get(0).getRating()).isEqualTo(4.8);
        assertThat(ranked.get(0).getRatingScore()).isGreaterThan(ranked.get(1).getRatingScore());
    }

    @Test
    @DisplayName("3. FASTEST_DELIVERY mode: Unknown delivery is NEVER treated as fastest")
    void testFastestDeliveryModeUnknownDeliverySafety() {
        NormalizedProductOfferDto unknownDelivery = NormalizedProductOfferDto.builder()
                .title("Samsung S24")
                .merchant("MerchantX")
                .price(new BigDecimal("59999"))
                .delivery(null) // Unknown delivery
                .deliveryEstimate(null)
                .build();

        NormalizedProductOfferDto standardDelivery = NormalizedProductOfferDto.builder()
                .title("Samsung S24")
                .merchant("Croma")
                .price(new BigDecimal("59999"))
                .delivery("Standard Delivery - 4 days")
                .build();

        NormalizedProductOfferDto nextDayDelivery = NormalizedProductOfferDto.builder()
                .title("Samsung S24")
                .merchant("Amazon")
                .price(new BigDecimal("60499"))
                .delivery("FREE Tomorrow by 11am")
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(unknownDelivery, standardDelivery, nextDayDelivery), "fastest_delivery");

        // Next Day delivery ranks #1
        assertThat(ranked.get(0).getMerchant()).isEqualTo("Amazon");
        assertThat(ranked.get(0).getIsFastestDelivery()).isTrue();
        assertThat(ranked.get(0).getRecommendationReason()).contains("day(s) earlier");

        // Unknown delivery ranks LAST among all offers (never treated as fastest)
        assertThat(ranked.get(2).getMerchant()).isEqualTo("MerchantX");
        assertThat(ranked.get(2).getDeliveryScore()).isLessThan(ranked.get(1).getDeliveryScore());
    }

    @Test
    @DisplayName("4. BEST_VALUE mode: Configurable 5-factor weighted score calculation")
    void testBestValueWeightedScoring() {
        NormalizedProductOfferDto expensiveTopStore = NormalizedProductOfferDto.builder()
                .title("Apple iPhone 15 Pro")
                .merchant("Amazon")
                .price(new BigDecimal("124900"))
                .rating(4.8)
                .reviewCount(5000)
                .delivery("FREE Tomorrow")
                .discountPercent(15)
                .inStock(true)
                .build();

        NormalizedProductOfferDto cheapPoorStore = NormalizedProductOfferDto.builder()
                .title("Apple iPhone 15 Pro")
                .merchant("SketchyStore")
                .price(new BigDecimal("119999")) // Cheaper by ₹5,000
                .rating(2.1) // Terrible rating
                .reviewCount(5)
                .delivery("Standard Courier (charges vary)")
                .discountPercent(0)
                .inStock(false) // Out of stock
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(expensiveTopStore, cheapPoorStore), "best_value");

        // Best Value combines price, rating, delivery, discount, availability
        assertThat(ranked.get(0).getMerchant()).isEqualTo("Amazon");
        assertThat(ranked.get(0).getIsBestValue()).isTrue();
        assertThat(ranked.get(0).getRankingLabel()).isEqualTo("BEST_VALUE");
        assertThat(ranked.get(0).getFinalScore()).isGreaterThan(ranked.get(1).getFinalScore());

        // Ranking Transparency scores are populated
        assertThat(ranked.get(0).getPriceScore()).isNotNull();
        assertThat(ranked.get(0).getRatingScore()).isNotNull();
        assertThat(ranked.get(0).getDeliveryScore()).isNotNull();
        assertThat(ranked.get(0).getDiscountScore()).isNotNull();
        assertThat(ranked.get(0).getAvailabilityScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("5. Missing attribute handling: Missing ratings are NOT replaced with 5.0")
    void testMissingRatingFairness() {
        NormalizedProductOfferDto unratedOffer = NormalizedProductOfferDto.builder()
                .title("Generic Cable")
                .merchant("StoreA")
                .price(new BigDecimal("299"))
                .rating(null) // Unrated
                .build();

        NormalizedProductOfferDto ratedOffer = NormalizedProductOfferDto.builder()
                .title("Generic Cable")
                .merchant("StoreB")
                .price(new BigDecimal("299"))
                .rating(4.5)
                .reviewCount(100)
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(unratedOffer, ratedOffer), "rating");

        // The rated offer with 4.5★ MUST score higher than unrated offer
        assertThat(ranked.get(0).getMerchant()).isEqualTo("StoreB");
        assertThat(unratedOffer.getRatingScore()).isLessThan(ratedOffer.getRatingScore());
    }

    @Test
    @DisplayName("6. Real calculated explanations and 'Why this option?' generation")
    void testExplanationGenerationFromRealValues() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .title("Sony Headphones")
                .merchant("Amazon")
                .price(new BigDecimal("26990"))
                .rating(4.5)
                .reviewCount(1200)
                .delivery("FREE Tomorrow")
                .discountPercent(20)
                .inStock(true)
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .title("Sony Headphones")
                .merchant("Flipkart")
                .price(new BigDecimal("29990"))
                .rating(4.0)
                .delivery("Standard Delivery")
                .inStock(true)
                .build();

        List<NormalizedProductOfferDto> ranked = rankingService.rankAndMarkCheapest(
                List.of(offer1, offer2), "best_value");

        NormalizedProductOfferDto best = ranked.get(0);
        assertThat(best.getMerchant()).isEqualTo("Amazon");

        // Explanation uses real numbers: avg price = 28490, diff = 1500
        assertThat(best.getRecommendationReason()).contains("below the average offer");
        assertThat(best.getRecommendationReason()).contains("rated 4.5/5");
        assertThat(best.getRecommendationReason()).contains("free delivery");

        // 'Why this option?' bullets contain verified values
        assertThat(best.getWhyThisOption()).anyMatch(r -> r.contains("below average offer") || r.contains("Lowest effective price"));
        assertThat(best.getWhyThisOption()).contains("4.5/5 rating (1200 reviews)");
        assertThat(best.getWhyThisOption()).contains("Free delivery");
        assertThat(best.getWhyThisOption()).contains("In stock");
        assertThat(best.getWhyThisOption()).contains("20% discount off MRP");
    }

    @Test
    @DisplayName("7. Tie-breaking and determinism: Same price, same rating produces deterministic ordering")
    void testDeterministicTies() {
        NormalizedProductOfferDto offerA = NormalizedProductOfferDto.builder()
                .title("SSD 1TB")
                .merchant("Amazon")
                .price(new BigDecimal("6999"))
                .rating(4.5)
                .reviewCount(500)
                .delivery("Tomorrow")
                .inStock(true)
                .build();

        NormalizedProductOfferDto offerB = NormalizedProductOfferDto.builder()
                .title("SSD 1TB")
                .merchant("Flipkart")
                .price(new BigDecimal("6999"))
                .rating(4.5)
                .reviewCount(500)
                .delivery("Tomorrow")
                .inStock(true)
                .build();

        List<NormalizedProductOfferDto> run1 = rankingService.rankAndMarkCheapest(List.of(offerA, offerB), "best_value");
        List<NormalizedProductOfferDto> run2 = rankingService.rankAndMarkCheapest(List.of(offerA, offerB), "best_value");

        assertThat(run1.get(0).getMerchant()).isEqualTo(run2.get(0).getMerchant());
        assertThat(run1.get(0).getFinalScore()).isEqualTo(run2.get(0).getFinalScore());
    }
}
