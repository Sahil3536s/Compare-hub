package com.comparehub.service;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.model.Product;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.impl.PriceHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DealQualityService dealQualityService;

    @Mock
    private PurchaseTimingService purchaseTimingService;

    @InjectMocks
    private PriceHistoryServiceImpl priceHistoryService;

    @Test
    void shouldNotRecordDuplicatePriceWithin24Hours() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        ProductPriceHistory existing = ProductPriceHistory.builder()
                .id(10L)
                .product(product)
                .merchant("Amazon")
                .price(new BigDecimal("79999.00"))
                .recordedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();

        when(priceHistoryRepository.findTopByProductIdAndMerchantOrderByRecordedAtDesc(1L, "Amazon"))
                .thenReturn(Optional.of(existing));

        priceHistoryService.recordPriceIfChanged(product, "Amazon", new BigDecimal("79999.00"), "INR");

        // Verify save is NOT called for duplicate price
        verify(priceHistoryRepository, never()).save(any(ProductPriceHistory.class));
    }

    @Test
    void shouldRecordPriceWhenPriceChanges() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        ProductPriceHistory existing = ProductPriceHistory.builder()
                .id(10L)
                .product(product)
                .merchant("Amazon")
                .price(new BigDecimal("79999.00"))
                .recordedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();

        when(priceHistoryRepository.findTopByProductIdAndMerchantOrderByRecordedAtDesc(1L, "Amazon"))
                .thenReturn(Optional.of(existing));

        priceHistoryService.recordPriceIfChanged(product, "Amazon", new BigDecimal("74999.00"), "INR");

        // Verify save IS called when price changes
        verify(priceHistoryRepository, times(1)).save(any(ProductPriceHistory.class));
    }

    @Test
    void shouldCalculatePriceHistoryMetricsAndAnalysis() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Instant now = Instant.now();
        List<ProductPriceHistory> history = List.of(
                ProductPriceHistory.builder().product(product).merchant("Amazon").price(new BigDecimal("80000.00")).recordedAt(now.minus(20, ChronoUnit.DAYS)).build(),
                ProductPriceHistory.builder().product(product).merchant("Amazon").price(new BigDecimal("75000.00")).recordedAt(now.minus(10, ChronoUnit.DAYS)).build(),
                ProductPriceHistory.builder().product(product).merchant("Amazon").price(new BigDecimal("70000.00")).recordedAt(now).build()
        );

        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(1L), any(Instant.class)))
                .thenReturn(history);

        ProductPriceHistoryResponseDto response = priceHistoryService.getPriceHistory(1L, "30D");

        assertNotNull(response);
        assertEquals("30D", response.getPeriod());
        assertEquals(new BigDecimal("70000.00"), response.getCurrentPrice());
        assertEquals(new BigDecimal("70000.00"), response.getLowestPrice());
        assertEquals(new BigDecimal("80000.00"), response.getHighestPrice());
        assertEquals(new BigDecimal("75000.00"), response.getAveragePrice());
        assertTrue(response.getAnalysisText().contains("below the 30-day average"));
    }
}
