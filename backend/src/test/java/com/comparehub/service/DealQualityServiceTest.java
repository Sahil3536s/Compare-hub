package com.comparehub.service;

import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.impl.DealQualityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DealQualityServiceTest {

    private DealQualityService dealQualityService;

    @BeforeEach
    void setUp() {
        dealQualityService = new DealQualityServiceImpl();
    }

    @Test
    @DisplayName("Should detect Exceptional Deal when price is at 90-day low and substantially below historical average")
    void testExceptionalDeal() {
        // Current: ₹45,000, 90D Low: ₹45,000, 30D Low: ₹47,000, Hist Avg: ₹52,000, MRP: ₹60,000
        DealQualityDto result = dealQualityService.calculateDealQuality(
                new BigDecimal("45000"),
                new BigDecimal("52000"),
                new BigDecimal("47000"),
                new BigDecimal("45000"),
                new BigDecimal("60000")
        );

        assertThat(result).isNotNull();
        assertThat(result.getDealScore()).isGreaterThanOrEqualTo(90);
        assertThat(result.getClassification()).isEqualTo("EXCEPTIONAL_DEAL");
        assertThat(result.getIsAtLowest()).isTrue();
        assertThat(result.getRealDiscountPercentVsAverage()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Should flag inflated MRP where advertised discount is 29% but real savings vs market average is small")
    void testInflatedMrpDisclaimer() {
        // Current: ₹49,999, Hist Avg: ₹51,200 (only ~2.3% saving), MRP: ₹69,999 (29% advertised)
        DealQualityDto result = dealQualityService.calculateDealQuality(
                new BigDecimal("49999"),
                new BigDecimal("51200"),
                new BigDecimal("49500"),
                new BigDecimal("48000"),
                new BigDecimal("69999")
        );

        assertThat(result).isNotNull();
        assertThat(result.getDealScore()).isBetween(40, 65);
        assertThat(result.getAdvertisedDiscountPercent()).isGreaterThanOrEqualTo(25);
        assertThat(result.getRealDiscountPercentVsAverage()).isLessThanOrEqualTo(4);
        assertThat(result.getDisclaimer()).isNotNull();
        assertThat(result.getDisclaimer()).contains("Advertised discount is high");
        assertThat(result.getDisclaimer()).contains("recent average");
    }

    @Test
    @DisplayName("Should evaluate offer deal quality from NormalizedProductOfferDto")
    void testEvaluateOfferDealQuality() {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .productName("Sony WH-1000XM5")
                .merchant("Amazon")
                .price(new BigDecimal("26990"))
                .originalPrice(new BigDecimal("34990"))
                .build();

        DealQualityDto result = dealQualityService.evaluateOfferDealQuality(offer);

        assertThat(result).isNotNull();
        assertThat(result.getDealScore()).isBetween(0, 100);
        assertThat(result.getClassificationLabel()).isNotBlank();
    }
}
