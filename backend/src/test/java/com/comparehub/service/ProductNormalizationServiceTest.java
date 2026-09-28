package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductNormalizationServiceTest {

    private ProductNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        ProductAttributeExtractor extractor = new ProductAttributeExtractorImpl();
        EffectiveCostService effectiveCostService = new EffectiveCostServiceImpl();
        OfferEligibilityService offerEligibilityService = new OfferEligibilityServiceImpl();
        PaymentOfferService paymentOfferService = new PaymentOfferServiceImpl(null, null);
        TrueCostService trueCostService = new TrueCostServiceImpl(offerEligibilityService, paymentOfferService);
        DealQualityService dealQualityService = new DealQualityServiceImpl();
        PurchaseTimingService purchaseTimingService = new PurchaseTimingServiceImpl();
        normalizationService = new ProductNormalizationServiceImpl(
                extractor, effectiveCostService, trueCostService, dealQualityService, purchaseTimingService);
    }

    @Test
    @DisplayName("Should normalize discount percentage, clean title, and normalize currency")
    void shouldCalculateCorrectDiscountPercentage() {
        NormalizedProductOfferDto raw = NormalizedProductOfferDto.builder()
                .productName("  Apple iPhone 15 Pro - Lowest Price Ever [Limited Time Deal]  ")
                .merchant("Amazon ")
                .price(new BigDecimal("120000.00"))
                .originalPrice(new BigDecimal("150000.00"))
                .currency("₹")
                .build();

        NormalizedProductOfferDto result = normalizationService.normalizeOffer(raw);

        assertNotNull(result);
        assertTrue(result.getProductName().contains("Apple iPhone 15 Pro"));
        assertFalse(result.getProductName().toLowerCase().contains("lowest price ever"));
        assertEquals("Amazon", result.getMerchant());
        assertEquals(20, result.getDiscountPercent());
        assertEquals("INR", result.getCurrency());
        assertNotNull(result.getEffectiveCost());
        assertNotNull(result.getCostBreakdown());
        assertNotNull(result.getDealQuality());
    }

    @Test
    @DisplayName("Should NEVER invent missing rating (rating must remain null if unavailable, NOT 4.5)")
    void shouldNeverInventMissingRating() {
        NormalizedProductOfferDto raw = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24")
                .merchant("Flipkart")
                .price(new BigDecimal("59999"))
                .rating(null) // Unrated product
                .build();

        NormalizedProductOfferDto result = normalizationService.normalizeOffer(raw);

        assertNotNull(result);
        assertNull(result.getRating(), "Missing rating must NEVER be defaulted to 4.5; must remain null!");
    }

    @Test
    @DisplayName("Should normalize price and storage when provided in unstructured formats")
    void shouldNormalizePriceAndAttributesFromOffer() {
        NormalizedProductOfferDto raw = NormalizedProductOfferDto.builder()
                .title("SAMSUNG Galaxy S24 5G (Onyx Black, 256 GB) (8 GB RAM)")
                .merchant("Croma")
                .price(new BigDecimal("58499"))
                .brand("SAMSUNG")
                .storage("256 GB")
                .ram("8 GB RAM")
                .build();

        NormalizedProductOfferDto result = normalizationService.normalizeOffer(raw);

        assertNotNull(result);
        assertEquals("Samsung", result.getBrand());
        assertEquals("256GB", result.getStorage());
        assertEquals("8GB", result.getRam());
        assertEquals("Galaxy S24", result.getModel());
        assertNotNull(result.getCanonicalKey());
    }

    @Test
    @DisplayName("Should handle null and empty raw offer safely")
    void shouldHandleNullAndEmptyOffersSafely() {
        assertNull(normalizationService.normalizeOffer(null));
        assertTrue(normalizationService.normalizeOffers(null).isEmpty());
    }
}
