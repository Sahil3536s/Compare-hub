package com.comparehub.service;

import com.comparehub.dto.PricePointDto;
import com.comparehub.dto.PurchaseTimingDto;
import com.comparehub.service.impl.PurchaseTimingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseTimingServiceTest {

    private PurchaseTimingService purchaseTimingService;

    @BeforeEach
    void setUp() {
        purchaseTimingService = new PurchaseTimingServiceImpl();
    }

    @Test
    @DisplayName("Should detect GOOD_TIME_TO_BUY when price is 8% below 30-day average and near 90-day low")
    void testGoodTimeToBuy() {
        Instant now = Instant.now();
        List<PricePointDto> points = new ArrayList<>();
        // 30-day average around ₹54,000, current price ₹49,999 (about 8% below avg)
        points.add(PricePointDto.builder().price(new BigDecimal("55000")).recordedAt(now.minus(28, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("54500")).recordedAt(now.minus(21, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("54000")).recordedAt(now.minus(14, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("52000")).recordedAt(now.minus(5, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("49999")).recordedAt(now).build());

        PurchaseTimingDto result = purchaseTimingService.analyzeTiming(new BigDecimal("49999"), points);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isGreaterThanOrEqualTo(70);
        assertThat(result.getStatus()).isIn("GOOD_TIME_TO_BUY", "STRONG_BUY_PRICE");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons().stream().anyMatch(r -> r.contains("below the 30-day average"))).isTrue();
        assertThat(result.getReasons().stream().anyMatch(r -> r.contains("decreased during the last week"))).isTrue();
        assertThat(result.getDisclaimer()).contains("does not guarantee future price movements");
    }

    @Test
    @DisplayName("Should detect CONSIDER_WAITING when price is higher than recent 30-day average")
    void testConsiderWaiting() {
        Instant now = Instant.now();
        List<PricePointDto> points = new ArrayList<>();
        points.add(PricePointDto.builder().price(new BigDecimal("40000")).recordedAt(now.minus(28, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("40500")).recordedAt(now.minus(14, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("42000")).recordedAt(now.minus(7, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(new BigDecimal("46000")).recordedAt(now).build());

        PurchaseTimingDto result = purchaseTimingService.analyzeTiming(new BigDecimal("46000"), points);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isLessThan(60);
        assertThat(result.getStatus()).isEqualTo("CONSIDER_WAITING");
        assertThat(result.getReasons().stream().anyMatch(r -> r.contains("above the recent 30-day average") || r.contains("spiked"))).isTrue();
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_DATA when too few price points are recorded")
    void testInsufficientData() {
        PurchaseTimingDto result = purchaseTimingService.analyzeTiming(new BigDecimal("50000"), List.of(
                PricePointDto.builder().price(new BigDecimal("50000")).recordedAt(Instant.now()).build()
        ));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(result.getReasons().get(0)).contains("Insufficient historical price records");
    }
}
