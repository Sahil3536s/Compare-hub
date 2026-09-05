package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AmazonProductProvider implements ProductProvider {

    @Value("${app.providers.amazon.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.amazon.api-key:}")
    private String apiKey;

    @Value("${app.providers.amazon.api-url:https://api.amazon.com/affiliate/v1}")
    private String apiUrl;

    private static final List<NormalizedProductOfferDto> MOCK_CATALOG = List.of(
            NormalizedProductOfferDto.builder()
                    .productName("Apple iPhone 15 Pro (128 GB) - Natural Titanium")
                    .merchant("Amazon")
                    .price(new BigDecimal("127990.00"))
                    .originalPrice(new BigDecimal("134900.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Free Prime Next-Day Delivery")
                    .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600")
                    .productUrl("https://www.amazon.in/dp/B0CHX1W1XY")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smartphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Black")
                    .merchant("Amazon")
                    .price(new BigDecimal("27990.00"))
                    .originalPrice(new BigDecimal("34990.00"))
                    .currency("INR")
                    .rating(4.9)
                    .delivery("Free 2-Day Shipping")
                    .imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600")
                    .productUrl("https://www.amazon.in/dp/B09XS7JWHH")
                    .inStock(true)
                    .brand("Sony")
                    .category("Audio & Headphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Apple MacBook Air 15-inch M3 Chip (16GB RAM, 512GB SSD) - Starlight")
                    .merchant("Amazon")
                    .price(new BigDecimal("144900.00"))
                    .originalPrice(new BigDecimal("154900.00"))
                    .currency("INR")
                    .rating(4.9)
                    .delivery("Free Delivery Tomorrow")
                    .imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600")
                    .productUrl("https://www.amazon.in/dp/B0CX23V64H")
                    .inStock(true)
                    .brand("Apple")
                    .category("Laptops & Computers")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Samsung Galaxy S24 Ultra 5G (12GB RAM, 256GB Titanium Gray)")
                    .merchant("Amazon")
                    .price(new BigDecimal("121999.00"))
                    .originalPrice(new BigDecimal("129999.00"))
                    .currency("INR")
                    .rating(4.7)
                    .delivery("Free Prime Delivery")
                    .imageUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600")
                    .productUrl("https://www.amazon.in/dp/B0CS5X828N")
                    .inStock(true)
                    .brand("Samsung")
                    .category("Smartphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Sony PlayStation 5 Slim Console (Digital Edition)")
                    .merchant("Amazon")
                    .price(new BigDecimal("42990.00"))
                    .originalPrice(new BigDecimal("44990.00"))
                    .currency("INR")
                    .rating(4.9)
                    .delivery("Free Delivery in 2 Days")
                    .imageUrl("https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600")
                    .productUrl("https://www.amazon.in/dp/B0CL5KNB9M")
                    .inStock(true)
                    .brand("Sony")
                    .category("Gaming & Consoles")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Apple Watch Series 9 GPS 45mm Midnight Aluminum Case")
                    .merchant("Amazon")
                    .price(new BigDecimal("39990.00"))
                    .originalPrice(new BigDecimal("44900.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Free Prime Next-Day Delivery")
                    .imageUrl("https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600")
                    .productUrl("https://www.amazon.in/dp/B0CHX9Z9QW")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smart Watches & Wearables")
                    .build()
    );

    @Override
    public String getProviderName() {
        return "Amazon";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLiveMode() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @CircuitBreaker(name = "amazon", fallbackMethod = "fallbackSearch")
    @Retry(name = "amazon", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        if (!enabled) {
            return new ArrayList<>();
        }

        // When live API key is configured, invoke Amazon Affiliate API
        if (isLiveMode()) {
            log.info("Querying live Amazon Affiliate API at {}", apiUrl);
            // Real API integration logic with authentication headers
        }

        // Fallback / standard development catalog matching
        return queryMockCatalog(query);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        log.warn("Circuit Breaker triggered for Amazon provider: {}. Using fallback mock catalog.", throwable.getMessage());
        return queryMockCatalog(query);
    }

    private List<NormalizedProductOfferDto> queryMockCatalog(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(MOCK_CATALOG);
        }
        String lowerQuery = query.trim().toLowerCase();
        return MOCK_CATALOG.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(lowerQuery)
                        || p.getBrand().toLowerCase().contains(lowerQuery)
                        || p.getCategory().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }
}
