package com.comparehub.service;

import com.comparehub.dto.SmartDealDto;
import com.comparehub.dto.SmartDealsPageDto;
import com.comparehub.model.*;
import com.comparehub.repository.*;
import com.comparehub.service.impl.DealDiscoveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealDiscoveryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Mock
    private SavedProductRepository savedProductRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    private DealDiscoveryService dealDiscoveryService;

    @BeforeEach
    void setUp() {
        dealDiscoveryService = new DealDiscoveryServiceImpl(
                productRepository,
                priceHistoryRepository,
                savedProductRepository,
                priceAlertRepository,
                searchHistoryRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("Should discover and categorize smart deals across products and travel")
    void testDiscoverSmartDeals() {
        Product laptop = Product.builder()
                .id(101L)
                .name("Gaming Laptop RTX 4070")
                .category("Electronics")
                .offers(new ArrayList<>())
                .build();

        MerchantOffer offer = MerchantOffer.builder()
                .id(501L)
                .product(laptop)
                .merchant("Amazon")
                .price(BigDecimal.valueOf(67999.00))
                .originalPrice(BigDecimal.valueOf(82999.00))
                .productUrl("https://amazon.in/laptop")
                .build();
        laptop.getOffers().add(offer);

        ProductPriceHistory h1 = ProductPriceHistory.builder()
                .product(laptop)
                .price(BigDecimal.valueOf(82999.00))
                .recordedAt(Instant.now().minusSeconds(86400 * 10))
                .build();
        ProductPriceHistory h2 = ProductPriceHistory.builder()
                .product(laptop)
                .price(BigDecimal.valueOf(81000.00))
                .recordedAt(Instant.now().minusSeconds(86400 * 5))
                .build();

        when(productRepository.findAllWithOffers()).thenReturn(List.of(laptop));
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(eq(101L), any()))
                .thenReturn(List.of(h1, h2));
        when(userRepository.findAll()).thenReturn(List.of(User.builder().id(1L).email("user@example.com").build()));

        List<SmartDealDto> deals = dealDiscoveryService.discoverSmartDeals();

        assertNotNull(deals);
        assertFalse(deals.isEmpty());

        // Verify product deal was discovered with high deal score
        SmartDealDto laptopDeal = deals.stream()
                .filter(d -> "Gaming Laptop RTX 4070".equals(d.getTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(laptopDeal);
        assertEquals(BigDecimal.valueOf(67999.0), laptopDeal.getCurrentPrice());
        assertTrue(laptopDeal.getDealScore() >= 80);
        assertEquals(SmartDealCategory.EXCEPTIONAL_DEALS, laptopDeal.getDealCategory());
        assertTrue(laptopDeal.getReason().contains("below 30-day average"));

        // Verify travel deals are included
        boolean hasTravelDeals = deals.stream().anyMatch(d -> d.getDealCategory() == SmartDealCategory.TRAVEL_DEALS);
        assertTrue(hasTravelDeals);
    }

    @Test
    @DisplayName("Should categorize price alert triggered item into WATCHLIST_DEALS")
    void testPriceAlertWatchlistMatch() {
        Product phone = Product.builder()
                .id(102L)
                .name("Samsung Galaxy S24")
                .category("Smartphones")
                .offers(new ArrayList<>())
                .build();

        MerchantOffer offer = MerchantOffer.builder()
                .id(502L)
                .product(phone)
                .merchant("Flipkart")
                .price(BigDecimal.valueOf(59999.00))
                .originalPrice(BigDecimal.valueOf(74999.00))
                .build();
        phone.getOffers().add(offer);

        User user = User.builder().id(1L).email("user@example.com").build();
        PriceAlert alert = PriceAlert.builder()
                .id(1L)
                .user(user)
                .product(phone)
                .targetPrice(BigDecimal.valueOf(60000.00))
                .active(true)
                .build();

        when(productRepository.findAllWithOffers()).thenReturn(List.of(phone));
        when(priceAlertRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(alert));
        when(userRepository.findAll()).thenReturn(List.of(user));

        SmartDealsPageDto result = dealDiscoveryService.getPersonalizedSmartDeals(SmartDealCategory.WATCHLIST_DEALS, PageRequest.of(0, 10));

        assertNotNull(result);
        assertNotNull(result.getDeals());
        assertTrue(result.getDeals().stream().anyMatch(d -> "Samsung Galaxy S24".equals(d.getTitle()) && d.isAlertTriggered()));
    }
}
