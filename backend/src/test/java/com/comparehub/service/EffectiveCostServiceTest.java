package com.comparehub.service;

import com.comparehub.dto.AppliedOfferDto;
import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.EffectiveCostResultDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.impl.EffectiveCostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EffectiveCostServiceTest {

    private EffectiveCostService effectiveCostService;

    @BeforeEach
    void setUp() {
        effectiveCostService = new EffectiveCostServiceImpl();
    }

    @Test
    @DisplayName("1. Effective cost calculation with known delivery charge")
    void testEffectiveCostWithKnownDeliveryCharge() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .title("Logitech Mouse")
                .price(new BigDecimal("1299.00"))
                .deliveryCost(new BigDecimal("50.00"))
                .delivery("Standard Delivery - ₹50")
                .currency("INR")
                .build();

        EffectiveCostResultDto result = effectiveCostService.calculateEffectiveCost(offer);

        assertThat(result.getBasePrice()).isEqualByComparingTo(new BigDecimal("1299.00"));
        assertThat(result.isDeliveryChargeKnown()).isTrue();
        assertThat(result.getDeliveryCharge()).isEqualByComparingTo(new BigDecimal("50.00"));
        // 1299 + 50 = 1349
        assertThat(result.getEffectiveCost()).isEqualByComparingTo(new BigDecimal("1349.00"));
    }

    @Test
    @DisplayName("2. Effective cost calculation with free delivery")
    void testEffectiveCostWithFreeDelivery() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24")
                .price(new BigDecimal("59999.00"))
                .delivery("FREE Delivery by Tomorrow")
                .currency("INR")
                .build();

        EffectiveCostResultDto result = effectiveCostService.calculateEffectiveCost(offer);

        assertThat(result.isDeliveryChargeKnown()).isTrue();
        assertThat(result.getDeliveryCharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getEffectiveCost()).isEqualByComparingTo(new BigDecimal("59999.00"));
    }

    @Test
    @DisplayName("3. Effective cost calculation with unknown delivery (MUST NOT assume 0)")
    void testEffectiveCostWithUnknownDelivery() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .title("Office Chair")
                .price(new BigDecimal("4999.00"))
                .delivery("Courier Delivery (charges vary)")
                .deliveryCost(null)
                .currency("INR")
                .build();

        EffectiveCostResultDto result = effectiveCostService.calculateEffectiveCost(offer);

        assertThat(result.isDeliveryChargeKnown()).isFalse();
        assertThat(result.getDeliveryCharge()).isNull();
        // Effective cost remains base price without assuming 0 delivery
        assertThat(result.getEffectiveCost()).isEqualByComparingTo(new BigDecimal("4999.00"));
        assertThat(result.getCalculationNotes()).anyMatch(note -> note.contains("Delivery charge unknown"));
    }

    @Test
    @DisplayName("4. Conditional discount preserved separately without altering unconditional base effective cost")
    void testConditionalBankDiscountPreservedSeparately() {
        CostBreakdownDto breakdown = CostBreakdownDto.builder()
                .basePrice(new BigDecimal("59999.00"))
                .appliedOffers(List.of(
                        AppliedOfferDto.builder()
                                .type("BANK_OFFER")
                                .description("Flat ₹3,000 Off on HDFC Credit Card")
                                .discountAmount(new BigDecimal("3000.00"))
                                .isConditional(true)
                                .terms("Valid only on HDFC Bank Credit Card non-EMI transactions")
                                .build(),
                        AppliedOfferDto.builder()
                                .type("COUPON")
                                .description("Flat ₹500 Instant Store Coupon")
                                .discountAmount(new BigDecimal("500.00"))
                                .isConditional(false)
                                .terms("Applicable for all users")
                                .build()
                ))
                .build();

        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .title("Samsung Galaxy S24 5G")
                .price(new BigDecimal("59999.00"))
                .delivery("FREE Delivery")
                .costBreakdown(breakdown)
                .currency("INR")
                .build();

        EffectiveCostResultDto result = effectiveCostService.calculateEffectiveCost(offer);

        // Confirmed unconditional discount is 500
        assertThat(result.getConfirmedDiscount()).isEqualByComparingTo(new BigDecimal("500.00"));
        // Effective cost = 59999 - 500 = 59499 (conditional 3000 is NOT deducted from base effective cost!)
        assertThat(result.getEffectiveCost()).isEqualByComparingTo(new BigDecimal("59499.00"));

        // Conditional offer is stored separately with conditionalOffer = true
        assertThat(result.getConditionalOffers()).hasSize(1);
        assertThat(result.getConditionalOffers().get(0).isConditionalOffer()).isTrue();
        assertThat(result.getConditionalOffers().get(0).getDescription()).contains("HDFC Credit Card");
        assertThat(result.getConditionalOffers().get(0).getDiscountAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("5. Null offer handling safety")
    void testNullOfferSafety() {
        EffectiveCostResultDto result = effectiveCostService.calculateEffectiveCost(null);

        assertThat(result).isNotNull();
        assertThat(result.getEffectiveCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.isDeliveryChargeKnown()).isFalse();
    }
}
