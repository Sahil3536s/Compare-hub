package com.comparehub.service;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.model.Product;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.impl.PriceHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PriceHistoryServiceImplTest {

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PriceHistoryServiceImpl service;

    private Product product;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        // product.setOffers(...) not needed for basic stats
    }

    @Test
    void getPriceHistory_withSufficientData_returnsStats() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        // create 5 price points over 5 days
        ProductPriceHistory p1 = new ProductPriceHistory();
        p1.setPrice(new BigDecimal("100"));
        p1.setRecordedAt(Instant.now().minusSeconds(4 * 86400));
        p1.setMerchant("Demo");
        ProductPriceHistory p2 = new ProductPriceHistory();
        p2.setPrice(new BigDecimal("90"));
        p2.setRecordedAt(Instant.now().minusSeconds(3 * 86400));
        p2.setMerchant("Demo");
        ProductPriceHistory p3 = new ProductPriceHistory();
        p3.setPrice(new BigDecimal("110"));
        p3.setRecordedAt(Instant.now().minusSeconds(2 * 86400));
        p3.setMerchant("Demo");
        ProductPriceHistory p4 = new ProductPriceHistory();
        p4.setPrice(new BigDecimal("95"));
        p4.setRecordedAt(Instant.now().minusSeconds(86400));
        p4.setMerchant("Demo");
        ProductPriceHistory p5 = new ProductPriceHistory();
        p5.setPrice(new BigDecimal("105"));
        p5.setRecordedAt(Instant.now());
        p5.setMerchant("Demo");

        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(1L), any()))
                .thenReturn(Arrays.asList(p1, p2, p3, p4, p5));

        ProductPriceHistoryResponseDto dto = service.getPriceHistory(1L, "7D");
        assertNotNull(dto);
        assertEquals(new BigDecimal("105"), dto.getCurrentPrice());
        assertEquals(new BigDecimal("90"), dto.getLowestPrice());
        assertEquals(new BigDecimal("110"), dto.getHighestPrice());
        assertTrue(dto.getAveragePrice().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(5, dto.getPricePoints().size());
    }

    @Test
    void getPriceHistory_noHistory_returnsPlaceholder() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        ProductPriceHistoryResponseDto dto = service.getPriceHistory(1L, "7D");
        assertNotNull(dto);
        assertEquals(BigDecimal.ZERO, dto.getLowestPrice());
        assertEquals("Price history is not available yet.", dto.getAnalysisText());
        assertTrue(dto.getPricePoints().isEmpty());
    }
}
