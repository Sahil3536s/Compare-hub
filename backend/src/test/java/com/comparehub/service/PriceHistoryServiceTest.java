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

    @Test
    void shouldDefaultProvenanceToUnknownAndUnverified() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        when(priceHistoryRepository.findTopByProductIdAndMerchantOrderByRecordedAtDesc(1L, "Amazon"))
                .thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<ProductPriceHistory> captor = org.mockito.ArgumentCaptor.forClass(ProductPriceHistory.class);

        priceHistoryService.recordPriceIfChanged(product, "Amazon", new BigDecimal("74999.00"), "INR");

        verify(priceHistoryRepository).save(captor.capture());
        ProductPriceHistory saved = captor.getValue();
        assertEquals("UNKNOWN", saved.getDataSource());
        assertEquals(false, saved.getIsLive());
        assertEquals("UNVERIFIED", saved.getProvenance());
    }

    @Test
    void shouldNeverUpgradeDemoOrUnknownDataToVerifiedLive() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        when(priceHistoryRepository.findTopByProductIdAndMerchantOrderByRecordedAtDesc(1L, "DemoMerchant"))
                .thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<ProductPriceHistory> captor = org.mockito.ArgumentCaptor.forClass(ProductPriceHistory.class);

        // Attempting to pass isLive=true with DEMO dataSource and VERIFIED_LIVE provenance
        priceHistoryService.recordPriceIfChanged(product, "DemoMerchant", new BigDecimal("74999.00"), "INR", "DEMO", true, "VERIFIED_LIVE");

        verify(priceHistoryRepository).save(captor.capture());
        ProductPriceHistory saved = captor.getValue();
        assertEquals("DEMO", saved.getDataSource());
        assertEquals(false, saved.getIsLive(), "Must not be live for DEMO data");
        assertEquals("DEMO_DATA", saved.getProvenance(), "Must not upgrade to VERIFIED_LIVE");
    }

    @Test
    void shouldAssignVerifiedLiveOnlyForGenuinelyLiveOffers() {
        Product product = Product.builder().id(1L).name("iPhone 15").build();
        when(priceHistoryRepository.findTopByProductIdAndMerchantOrderByRecordedAtDesc(1L, "LiveMerchant"))
                .thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<ProductPriceHistory> captor = org.mockito.ArgumentCaptor.forClass(ProductPriceHistory.class);

        priceHistoryService.recordPriceIfChanged(product, "LiveMerchant", new BigDecimal("74999.00"), "INR", "LIVE", true, "VERIFIED_LIVE");

        verify(priceHistoryRepository).save(captor.capture());
        ProductPriceHistory saved = captor.getValue();
        assertEquals("LIVE", saved.getDataSource());
        assertEquals(true, saved.getIsLive());
        assertEquals("VERIFIED_LIVE", saved.getProvenance());
    }
}
