package com.comparehub.service;

import com.comparehub.client.MLServiceClient;
import com.comparehub.dto.MLServiceResponseDto;
import com.comparehub.dto.PricePredictionResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.Product;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.impl.PricePredictionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricePredictionServiceTest {

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MLServiceClient mlServiceClient;

    @InjectMocks
    private PricePredictionServiceImpl service;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder().id(1L).name("iPhone 15 Pro").build();
        // Set minHistoryPoints via reflection (simulates @Value injection)
        ReflectionTestUtils.setField(service, "minHistoryPoints", 20);
    }

    // ── Product not found ──

    @Test
    void shouldThrowResourceNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPricePrediction(999L));
    }

    // ── Insufficient history ──

    @Test
    void shouldReturnInsufficientDataWhenHistoryBelowMinimum() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(5)); // Only 5 points, need 20

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(5, result.getDataPointsUsed());
        assertNotNull(result.getMessage());
        verifyNoInteractions(mlServiceClient); // ML service must NOT be called
    }

    @Test
    void shouldReturnInsufficientDataWhenHistoryIsEmpty() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(List.of());

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(0, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    // ── ML service success ──

    @Test
    void shouldReturnSuccessPredictionWhenMLServiceResponds() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto mlResponse = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(58999.0)
                .predictedPrice(56500.0)
                .predictedChange(-2499.0)
                .predictedChangePercent(-4.24)
                .recommendation("WAIT")
                .recommendationReason("Price may decrease tomorrow.")
                .modelName("RandomForestRegressor")
                .modelVersion("1.0")
                .dataPointsUsed(25)
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(mlResponse));

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("iPhone 15 Pro", result.getProductName());
        assertEquals("WAIT", result.getRecommendation());
        assertEquals(new BigDecimal("58999.0"), result.getCurrentPrice());
        assertEquals(new BigDecimal("56500.0"), result.getPredictedPrice());
        assertEquals("RandomForestRegressor", result.getModelName());
    }

    // ── ML service unavailable ──

    @Test
    void shouldReturnMLUnavailableWhenMLServiceIsDown() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));
        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.empty());

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("TEMPORARILY_UNAVAILABLE", result.getStatus());
        assertNotNull(result.getMessage());
        assertNull(result.getPredictedPrice());
    }

    @Test
    void shouldRejectNegativePriceInMLResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto malformed = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(-500.0) // Impossible negative price!
                .predictedPrice(56000.0)
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(malformed));

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INVALID_RESPONSE", result.getStatus());
        assertNull(result.getPredictedPrice());
    }

    @Test
    void shouldRejectNaNPriceInMLResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto malformed = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(Double.NaN) // Impossible NaN!
                .predictedPrice(56000.0)
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(malformed));

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INVALID_RESPONSE", result.getStatus());
        assertNull(result.getPredictedPrice());
    }

    @Test
    void shouldRejectNegativePredictedPriceInMLResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto malformed = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(55000.0)
                .predictedPrice(-100.0) // Impossible negative predicted price!
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(malformed));

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INVALID_RESPONSE", result.getStatus());
        assertNull(result.getPredictedPrice());
    }

    @Test
    void shouldFilterOutDemoAndUnverifiedProvenanceFromHistory() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        List<ProductPriceHistory> mixedHistory = new ArrayList<>();
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);

        // 15 verified genuine live points
        for (int i = 0; i < 15; i++) {
            mixedHistory.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Amazon")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("LIVE")
                    .isLive(true)
                    .provenance("VERIFIED_LIVE")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }
        // 10 demo/mock points with DEMO provenance
        for (int i = 15; i < 25; i++) {
            mixedHistory.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Demo Merchant")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("DEMO")
                    .isLive(false)
                    .provenance("DEMO_DATA")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }

        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L)).thenReturn(mixedHistory);

        // Since only 15 genuine points exist (< 20), it should return INSUFFICIENT_DATA and NOT call ML service
        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(15, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void shouldFailSafelyForUnknownProvenance() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        List<ProductPriceHistory> unknownHistory = new ArrayList<>();
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);

        // 25 points with UNKNOWN / missing structured provenance
        for (int i = 0; i < 25; i++) {
            unknownHistory.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Unknown Vendor")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("UNKNOWN")
                    .isLive(false)
                    .provenance("UNVERIFIED")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }

        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L)).thenReturn(unknownHistory);

        // Unknown provenance must fail safely and be rejected
        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(0, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void shouldRejectObservationsWhenProvenanceNotVerifiedLiveEvenIfIsLiveTrueAndDataSourceLive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        List<ProductPriceHistory> history = new ArrayList<>();
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);

        // 25 points with isLive=true, dataSource="LIVE", but provenance="UNVERIFIED"
        for (int i = 0; i < 25; i++) {
            history.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Amazon")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("LIVE")
                    .isLive(true)
                    .provenance("UNVERIFIED") // Not VERIFIED_LIVE
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }

        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L)).thenReturn(history);

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(0, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void shouldRejectObservationsWhenIsLiveFalseEvenIfProvenanceVerifiedLive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        List<ProductPriceHistory> history = new ArrayList<>();
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);

        // 25 points with isLive=false, dataSource="LIVE", provenance="VERIFIED_LIVE"
        for (int i = 0; i < 25; i++) {
            history.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Amazon")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("LIVE")
                    .isLive(false)
                    .provenance("VERIFIED_LIVE")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }

        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L)).thenReturn(history);

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(0, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void shouldRejectObservationsWhenDataSourceNotLiveEvenIfProvenanceVerifiedLive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        List<ProductPriceHistory> history = new ArrayList<>();
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);

        // 25 points with isLive=true, dataSource="DEMO", provenance="VERIFIED_LIVE"
        for (int i = 0; i < 25; i++) {
            history.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Amazon")
                    .price(new BigDecimal("50000.00"))
                    .currency("INR")
                    .dataSource("DEMO")
                    .isLive(true)
                    .provenance("VERIFIED_LIVE")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }

        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L)).thenReturn(history);

        PricePredictionResponseDto result = service.getPricePrediction(1L);

        assertEquals("INSUFFICIENT_DATA", result.getStatus());
        assertEquals(0, result.getDataPointsUsed());
        verifyNoInteractions(mlServiceClient);
    }

    // ── Recommendation values ──

    @Test
    void shouldCorrectlyMapBuyNowRecommendation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto mlResponse = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(55000.0)
                .predictedPrice(58000.0)
                .predictedChange(3000.0)
                .predictedChangePercent(5.45)
                .recommendation("BUY_NOW")
                .modelName("RandomForestRegressor")
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(mlResponse));

        PricePredictionResponseDto result = service.getPricePrediction(1L);
        assertEquals("BUY_NOW", result.getRecommendation());
    }

    @Test
    void shouldCorrectlyMapHoldRecommendation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(1L))
                .thenReturn(buildHistory(25));

        MLServiceResponseDto mlResponse = MLServiceResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .currentPrice(58000.0)
                .predictedPrice(58100.0)
                .predictedChange(100.0)
                .predictedChangePercent(0.17)
                .recommendation("HOLD")
                .modelName("LinearRegression")
                .build();

        when(mlServiceClient.predict(eq(1L), anyList())).thenReturn(Optional.of(mlResponse));

        PricePredictionResponseDto result = service.getPricePrediction(1L);
        assertEquals("HOLD", result.getRecommendation());
    }

    // ── Helpers ──

    private List<ProductPriceHistory> buildHistory(int count) {
        List<ProductPriceHistory> list = new ArrayList<>();
        Instant base = Instant.now().minus(count, ChronoUnit.DAYS);
        for (int i = 0; i < count; i++) {
            list.add(ProductPriceHistory.builder()
                    .id((long) i)
                    .product(testProduct)
                    .merchant("Amazon")
                    .price(new BigDecimal("58999.00"))
                    .currency("INR")
                    .dataSource("LIVE")
                    .isLive(true)
                    .provenance("VERIFIED_LIVE")
                    .recordedAt(base.plus(i, ChronoUnit.DAYS))
                    .build());
        }
        return list;
    }
}
