package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductNormalizationServiceTest {

    private ProductNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        OfferEligibilityService offerEligibilityService = new OfferEligibilityServiceImpl();
        PaymentOfferService paymentOfferService = new PaymentOfferServiceImpl(null, null);
        TrueCostService trueCostService = new TrueCostServiceImpl(offerEligibilityService, paymentOfferService);
        DealQualityService dealQualityService = new DealQualityServiceImpl();
        PurchaseTimingService purchaseTimingService = new PurchaseTimingServiceImpl();
        normalizationService = new ProductNormalizationServiceImpl(trueCostService, dealQualityService, purchaseTimingService);
    }

    @Test
    void shouldCalculateCorrectDiscountPercentage() {
        NormalizedProductOfferDto raw = NormalizedProductOfferDto.builder()
                .productName("  Apple iPhone 15 Pro  ")
                .merchant("Amazon ")
                .price(new BigDecimal("120000.00"))
                .originalPrice(new BigDecimal("150000.00"))
                .build();

        NormalizedProductOfferDto result = normalizationService.normalizeOffer(raw);

        assertNotNull(result);
        assertEquals("Apple iPhone 15 Pro", result.getProductName());
        assertEquals("Amazon", result.getMerchant());
        assertEquals(20, result.getDiscountPercent()); // (150000 - 120000) / 150000 = 20%
        assertEquals("INR", result.getCurrency());
        assertNotNull(result.getCostBreakdown());
        assertNotNull(result.getEffectivePrice());
        assertNotNull(result.getDealQuality());
        assertNotNull(result.getPurchaseTiming());
    }

    @Test
    void shouldHandleZeroDiscountGracefully() {
        NormalizedProductOfferDto raw = NormalizedProductOfferDto.builder()
                .productName("MacBook Air")
                .merchant("Flipkart")
                .price(new BigDecimal("99000.00"))
                .originalPrice(new BigDecimal("99000.00"))
                .build();

        NormalizedProductOfferDto result = normalizationService.normalizeOffer(raw);

        assertNotNull(result);
        assertEquals(0, result.getDiscountPercent());
    }
}
