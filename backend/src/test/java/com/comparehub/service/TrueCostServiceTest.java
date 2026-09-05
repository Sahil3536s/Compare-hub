package com.comparehub.service;

import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.impl.OfferEligibilityServiceImpl;
import com.comparehub.service.impl.PaymentOfferServiceImpl;
import com.comparehub.service.impl.TrueCostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TrueCostServiceTest {

    private TrueCostService trueCostService;

    @BeforeEach
    void setUp() {
        OfferEligibilityService offerEligibilityService = new OfferEligibilityServiceImpl();
        PaymentOfferService paymentOfferService = new PaymentOfferServiceImpl(null, null);
        trueCostService = new TrueCostServiceImpl(offerEligibilityService, paymentOfferService);
    }

    @Test
    @DisplayName("Should correctly calculate effective price with delivery and conditional bank discount")
    void testCalculateTrueCostWithDiscount() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .productName("Samsung Galaxy S24 Ultra 256GB")
                .merchant("Amazon")
                .price(new BigDecimal("120000"))
                .delivery("Free Delivery by Tomorrow")
                .category("Electronics")
                .build();

        CostBreakdownDto breakdown = trueCostService.calculateTrueCost(offer);

        assertThat(breakdown).isNotNull();
        assertThat(breakdown.getBasePrice()).isEqualByComparingTo(new BigDecimal("120000"));
        assertThat(breakdown.getDeliveryFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.getDiscounts()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(breakdown.getEffectivePrice()).isEqualByComparingTo(new BigDecimal("118500"));
        assertThat(breakdown.getHasConditionalDiscounts()).isTrue();
        assertThat(breakdown.getAppliedOffers()).isNotEmpty();
    }

    @Test
    @DisplayName("Should add delivery fee when order amount is below merchant threshold")
    void testCalculateTrueCostWithDeliveryFee() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .productName("USB-C Cable")
                .merchant("Flipkart")
                .price(new BigDecimal("299"))
                .delivery("Standard Delivery")
                .category("Accessories")
                .build();

        CostBreakdownDto breakdown = trueCostService.calculateTrueCost(offer);

        assertThat(breakdown).isNotNull();
        assertThat(breakdown.getBasePrice()).isEqualByComparingTo(new BigDecimal("299"));
        assertThat(breakdown.getDeliveryFee()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(breakdown.getPlatformFee()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(breakdown.getEffectivePrice()).isEqualByComparingTo(new BigDecimal("342"));
    }
}
