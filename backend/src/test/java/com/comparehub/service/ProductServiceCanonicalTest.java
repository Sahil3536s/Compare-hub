package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.service.DealQualityService;
import com.comparehub.service.PurchaseTimingService;
import com.comparehub.model.MerchantOffer;
import com.comparehub.model.Product;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.MerchantOfferRepository;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.impl.PriceHistoryServiceImpl;
import com.comparehub.service.impl.ProductPersistenceServiceImpl;
import com.comparehub.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceCanonicalTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MerchantOfferRepository merchantOfferRepository;

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Mock
private DealQualityService dealQualityService;

@Mock
private PurchaseTimingService purchaseTimingService;

    @Mock
    private ProductAttributeExtractor attributeExtractor;

    @Mock
    private ProductAlternativeService alternativeService;

    @Mock
    private PricePredictionService pricePredictionService;

    private PriceHistoryServiceImpl priceHistoryService;
    private ProductServiceImpl productService;
    private ProductPersistenceServiceImpl persistenceService;

    @BeforeEach
    void setUp() {
        priceHistoryService = new PriceHistoryServiceImpl(priceHistoryRepository, productRepository,
        dealQualityService,
        purchaseTimingService);
        productService = new ProductServiceImpl(
                productRepository,
                merchantOfferRepository,
                priceHistoryService,
                attributeExtractor,
                alternativeService,
                pricePredictionService
        );
        persistenceService = new ProductPersistenceServiceImpl(
                productRepository,
                merchantOfferRepository,
                priceHistoryService
        );
    }

    @Test
    void testCalculatePriceMeter_WithHistoricalData() {
        Long productId = 100L;
        Instant now = Instant.now();
        Product product = Product.builder().id(productId).name("Samsung S24 Ultra").build();

        List<ProductPriceHistory> history = List.of(
                ProductPriceHistory.builder().id(1L).product(product).merchant("Amazon").price(new BigDecimal("110000")).recordedAt(now.minusSeconds(86400 * 5)).build(),
                ProductPriceHistory.builder().id(2L).product(product).merchant("Flipkart").price(new BigDecimal("120000")).recordedAt(now.minusSeconds(86400 * 3)).build(),
                ProductPriceHistory.builder().id(3L).product(product).merchant("Croma").price(new BigDecimal("130000")).recordedAt(now.minusSeconds(86400)).build()
        );

        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(productId), any(Instant.class)))
                .thenReturn(history);

        // When current price is 112000 (near lowest 110000, avg 120000)
        PriceMeterDto meter = priceHistoryService.calculatePriceMeter(productId, new BigDecimal("112000"), "30D");

        assertNotNull(meter);
        assertEquals("CALCULATED", meter.getStatus());
        assertEquals(0, new BigDecimal("110000").compareTo(meter.getHistoricalLowest()));
        assertEquals(0, new BigDecimal("130000").compareTo(meter.getHistoricalHighest()));
        assertTrue(meter.getDifferencePercentage() < 0, "Current price is below average");
        assertEquals("GOOD_PRICE", meter.getClassification());
        assertNotNull(meter.getAdvice());
    }

    @Test
    void testCalculatePriceMeter_InsufficientData() {
        Long productId = 101L;
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(productId), any(Instant.class)))
                .thenReturn(List.of());

        PriceMeterDto meter = priceHistoryService.calculatePriceMeter(productId, new BigDecimal("50000"), "30D");

        assertNotNull(meter);
        assertEquals("INSUFFICIENT_DATA", meter.getStatus());
        assertEquals("INSUFFICIENT_DATA", meter.getClassification());
        assertNull(meter.getScore());
        assertFalse(Boolean.TRUE.equals(meter.getHasSufficientData()));
    }

    @Test
    void testCalculatePriceMeter_FewerThanThreeObservations_ReturnsInsufficientData() {
        Long productId = 105L;
        Instant now = Instant.now();
        Product product = Product.builder().id(productId).name("OnePlus 12").build();

        // Exactly 2 observations (< 3 threshold)
        List<ProductPriceHistory> history = List.of(
                ProductPriceHistory.builder().id(1L).product(product).merchant("Amazon").price(new BigDecimal("64999")).recordedAt(now.minusSeconds(86400 * 5)).build(),
                ProductPriceHistory.builder().id(2L).product(product).merchant("Flipkart").price(new BigDecimal("65999")).recordedAt(now.minusSeconds(86400 * 2)).build()
        );

        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(productId), any(Instant.class)))
                .thenReturn(history);

        PriceMeterDto meter = priceHistoryService.calculatePriceMeter(productId, new BigDecimal("64999"), "30D");

        assertNotNull(meter);
        assertEquals("INSUFFICIENT_DATA", meter.getStatus());
        assertEquals("INSUFFICIENT_DATA", meter.getClassification());
        assertEquals("Not enough price history yet", meter.getClassificationLabel());
        assertEquals("Not enough price history yet", meter.getSummaryText());
        assertNull(meter.getScore(), "Score must be null when fewer than 3 observations exist");
        assertFalse(Boolean.TRUE.equals(meter.getHasSufficientData()));
        assertEquals(2, meter.getObservationsCount());
    }

    @Test
    void testCalculatePriceMeter_DuplicateSameInstantRecords_DeduplicatedToInsufficient() {
        Long productId = 106L;
        Instant sameInstant = Instant.now().minusSeconds(3600);
        Product product = Product.builder().id(productId).name("iQOO 12").build();

        // 3 records but at the exact same second with the same price (duplicated poll)
        List<ProductPriceHistory> history = List.of(
                ProductPriceHistory.builder().id(1L).product(product).merchant("Amazon").price(new BigDecimal("52999")).recordedAt(sameInstant).build(),
                ProductPriceHistory.builder().id(2L).product(product).merchant("Amazon").price(new BigDecimal("52999")).recordedAt(sameInstant.plusSeconds(5)).build(),
                ProductPriceHistory.builder().id(3L).product(product).merchant("Amazon").price(new BigDecimal("52999")).recordedAt(sameInstant.plusSeconds(10)).build()
        );

        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(productId), any(Instant.class)))
                .thenReturn(history);

        PriceMeterDto meter = priceHistoryService.calculatePriceMeter(productId, new BigDecimal("52999"), "30D");

        assertNotNull(meter);
        assertEquals("INSUFFICIENT_DATA", meter.getStatus());
        assertEquals("INSUFFICIENT_DATA", meter.getClassification());
        assertNull(meter.getScore(), "Score must be null when distinct observations < 3");
        assertFalse(Boolean.TRUE.equals(meter.getHasSufficientData()));
        assertEquals(1, meter.getObservationsCount(), "Duplicate polls within minutes should collapse to 1 observation");
    }

    @Test
    void testGetCanonicalProductDetail_BundlesAllSections() {
        Long productId = 102L;
        Product product = Product.builder()
                .id(productId)
                .name("Apple iPhone 15 128GB Black")
                .brand("Apple")
                .category("Smartphones")
                .imageUrl("https://images.example.com/iphone15.jpg")
                .createdAt(Instant.now())
                .build();

        MerchantOffer offer1 = MerchantOffer.builder()
                .id(1L)
                .product(product)
                .merchant("Amazon")
                .price(new BigDecimal("69999.00"))
                .productUrl("https://amazon.in/iphone15")
                .inStock(true)
                .rating(new BigDecimal("4.6"))
                .build();

        MerchantOffer offer2 = MerchantOffer.builder()
                .id(2L)
                .product(product)
                .merchant("Flipkart")
                .price(new BigDecimal("71999.00"))
                .productUrl("https://flipkart.com/iphone15")
                .inStock(true)
                .rating(new BigDecimal("4.5"))
                .build();

        when(productRepository.findByIdWithOffers(productId)).thenReturn(Optional.of(product));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(merchantOfferRepository.findByProductIdOrderByPriceAsc(productId)).thenReturn(List.of(offer1, offer2));
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(productId), any(Instant.class)))
                .thenReturn(List.of());
        when(attributeExtractor.extractAttributes(any(), any(), any()))
                .thenReturn(ProductAttributesDto.builder()
                        .brand("Apple")
                        .model("iPhone 15")
                        .storage("128GB")
                        .color("Black")
                        .build());
        when(alternativeService.getAlternatives(any(), any(), any(), any(), anyInt())).thenReturn(null);

        CanonicalProductDetailDto detail = productService.getCanonicalProductDetail(productId);

        assertNotNull(detail);
        assertEquals(productId, detail.getId());
        assertEquals("Apple iPhone 15 128GB Black", detail.getName());
        assertEquals("Apple", detail.getBrand());
        assertEquals(new BigDecimal("69999.00"), detail.getCurrentLowestPrice());
        assertEquals("Amazon", detail.getCheapestMerchant());
        assertEquals(2, detail.getOffers().size());
        assertEquals(Boolean.TRUE, detail.getOffers().get(0).getIsCheapest());
        assertNotNull(detail.getSpecifications());
        assertEquals("128GB", detail.getSpecifications().get("Storage"));
        assertNotNull(detail.getPriceMeter());
    }

    @Test
    void testPersistenceService_DoesNotPollutePriceHistoryWithDemoData() {
        Product product = Product.builder()
                .id(200L)
                .name("Sony WH-1000XM5")
                .brand("Sony")
                .category("Audio")
                .build();

        when(productRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.of(product));
        when(merchantOfferRepository.findByProductIdOrderByPriceAsc(200L)).thenReturn(new ArrayList<>());

        // Group with 1 LIVE offer and 1 DEMO offer
        NormalizedProductOfferDto liveOffer = NormalizedProductOfferDto.builder()
                .merchant("Amazon")
                .price(new BigDecimal("26990.00"))
                .dataSource("LIVE")
                .live(true)
                .build();

        NormalizedProductOfferDto demoOffer = NormalizedProductOfferDto.builder()
                .merchant("DemoStore")
                .price(new BigDecimal("19999.00"))
                .dataSource("DEMO")
                .live(false)
                .build();

        CanonicalProductGroupDto group = CanonicalProductGroupDto.builder()
                .canonicalTitle("Sony WH-1000XM5")
                .brand("Sony")
                .category("Audio")
                .offers(List.of(liveOffer, demoOffer))
                .build();

        persistenceService.persistCanonicalGroupsAndOffers(List.of(group));

        // Verify MerchantOffer saved for both
        verify(merchantOfferRepository, times(2)).save(any(MerchantOffer.class));

        // Verify that priceHistoryRepository.save was NOT called for demo data, only at most once for live data!
        // (PriceHistoryServiceImpl.recordPriceIfChanged checks and saves)
        verify(priceHistoryRepository, atMost(1)).save(any(ProductPriceHistory.class));
    }
}
