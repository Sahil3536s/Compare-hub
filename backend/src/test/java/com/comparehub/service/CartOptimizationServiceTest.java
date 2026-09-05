package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.service.impl.CartOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartOptimizationServiceTest {

    @Mock
    private ProductComparisonService productComparisonService;

    @Mock
    private OfferEligibilityService offerEligibilityService;

    private CartOptimizationService cartOptimizationService;

    @BeforeEach
    void setUp() {
        cartOptimizationService = new CartOptimizationServiceImpl(productComparisonService, offerEligibilityService);
    }

    @Test
    @DisplayName("Should find cheapest mixed cart and calculate savings against single-store baseline")
    void testOptimizeCartMixedSavings() {
        // Item 1: Wireless Mouse
        // Amazon: ₹1,500, Flipkart: ₹2,000
        NormalizedProductOfferDto mouseAmazon = NormalizedProductOfferDto.builder()
                .productName("Logitech MX Master 3S")
                .merchant("Amazon")
                .price(new BigDecimal("1500"))
                .effectivePrice(new BigDecimal("1500"))
                .build();
        NormalizedProductOfferDto mouseFlipkart = NormalizedProductOfferDto.builder()
                .productName("Logitech MX Master 3S")
                .merchant("Flipkart")
                .price(new BigDecimal("2000"))
                .effectivePrice(new BigDecimal("2000"))
                .build();

        when(productComparisonService.compareProducts(eq("Mouse"), any(), any(), any(), any(), any(), eq(true), eq("best")))
                .thenReturn(ProductComparisonResponseDto.builder().offers(List.of(mouseAmazon, mouseFlipkart)).build());

        // Item 2: Mechanical Keyboard
        // Amazon: ₹6,000, Flipkart: ₹5,000
        NormalizedProductOfferDto kbAmazon = NormalizedProductOfferDto.builder()
                .productName("Keychron K2")
                .merchant("Amazon")
                .price(new BigDecimal("6000"))
                .effectivePrice(new BigDecimal("6000"))
                .build();
        NormalizedProductOfferDto kbFlipkart = NormalizedProductOfferDto.builder()
                .productName("Keychron K2")
                .merchant("Flipkart")
                .price(new BigDecimal("5000"))
                .effectivePrice(new BigDecimal("5000"))
                .build();

        when(productComparisonService.compareProducts(eq("Keyboard"), any(), any(), any(), any(), any(), eq(true), eq("best")))
                .thenReturn(ProductComparisonResponseDto.builder().offers(List.of(kbAmazon, kbFlipkart)).build());

        // Delivery fee mock (Orders >= 500 free)
        when(offerEligibilityService.determineDeliveryFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(offerEligibilityService.determinePlatformFee(eq("Amazon"))).thenReturn(BigDecimal.ZERO);
        when(offerEligibilityService.determinePlatformFee(eq("Flipkart"))).thenReturn(new BigDecimal("3"));

        CartOptimizationRequestDto request = CartOptimizationRequestDto.builder()
                .items(List.of(
                        CartItemDto.builder().name("Mouse").quantity(1).build(),
                        CartItemDto.builder().name("Keyboard").quantity(1).build()
                ))
                .strategy("MINIMIZE_PRICE")
                .build();

        CartOptimizationResponseDto response = cartOptimizationService.optimizeCart(request);

        assertThat(response).isNotNull();
        assertThat(response.getTotalItemsRequested()).isEqualTo(2);
        assertThat(response.getSingleStorePlans()).hasSize(2); // Amazon (₹7,500), Flipkart (₹7,003)

        // Single store baseline: Flipkart is ₹7,003
        assertThat(response.getCheapestSingleStore()).isNotNull();
        assertThat(response.getCheapestSingleStore().getGrandTotal()).isEqualByComparingTo(new BigDecimal("7003"));

        // Optimized Mixed: Mouse from Amazon (₹1,500) + Keyboard from Flipkart (₹5,000 + ₹3 fee) = ₹6,503
        assertThat(response.getOptimizedMixedPlan()).isNotNull();
        assertThat(response.getOptimizedMixedPlan().getGrandTotal()).isEqualByComparingTo(new BigDecimal("6503"));
        assertThat(response.getOptimizedMixedPlan().getTotalOrders()).isEqualTo(2);

        // Savings: ₹7,003 - ₹6,503 = ₹500
        assertThat(response.getEstimatedSavings()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(response.getRecommendedPlan().getPlanType()).isEqualTo("OPTIMIZED_MIXED");
    }

    @Test
    @DisplayName("Should recommend single-store plan when MINIMIZE_DELIVERIES strategy is requested")
    void testOptimizeCartMinimizeDeliveries() {
        NormalizedProductOfferDto mouseAmazon = NormalizedProductOfferDto.builder()
                .productName("Mouse")
                .merchant("Amazon")
                .price(new BigDecimal("1000"))
                .effectivePrice(new BigDecimal("1000"))
                .build();
        NormalizedProductOfferDto mouseFlipkart = NormalizedProductOfferDto.builder()
                .productName("Mouse")
                .merchant("Flipkart")
                .price(new BigDecimal("1100"))
                .effectivePrice(new BigDecimal("1100"))
                .build();

        when(productComparisonService.compareProducts(eq("Mouse"), any(), any(), any(), any(), any(), eq(true), eq("best")))
                .thenReturn(ProductComparisonResponseDto.builder().offers(List.of(mouseAmazon, mouseFlipkart)).build());

        when(offerEligibilityService.determineDeliveryFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(offerEligibilityService.determinePlatformFee(any())).thenReturn(BigDecimal.ZERO);

        CartOptimizationRequestDto request = CartOptimizationRequestDto.builder()
                .items(List.of(CartItemDto.builder().name("Mouse").quantity(1).build()))
                .strategy("MINIMIZE_DELIVERIES")
                .build();

        CartOptimizationResponseDto response = cartOptimizationService.optimizeCart(request);

        assertThat(response.getRecommendedPlan()).isNotNull();
        assertThat(response.getRecommendedPlan().getTotalOrders()).isEqualTo(1);
    }
}
