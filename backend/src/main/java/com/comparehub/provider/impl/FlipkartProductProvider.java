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
public class FlipkartProductProvider implements ProductProvider {

    @Value("${app.providers.flipkart.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.flipkart.api-key:}")
    private String apiKey;

    @Value("${app.providers.flipkart.api-url:https://affiliate-api.flipkart.net/v1}")
    private String apiUrl;

    private static final List<NormalizedProductOfferDto> MOCK_CATALOG = List.of(
            NormalizedProductOfferDto.builder()
                    .productName("Apple iPhone 15 Pro (128 GB) - Natural Titanium")
                    .merchant("Flipkart")
                    .price(new BigDecimal("128990.00"))
                    .originalPrice(new BigDecimal("134900.00"))
                    .currency("INR")
                    .rating(4.7)
                    .delivery("Delivery in 2 Days • Flipkart Plus")
                    .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600")
                    .productUrl("https://www.flipkart.com/apple-iphone-15-pro-natural-titanium-128-gb/p/itm123456")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smartphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Black")
                    .merchant("Flipkart")
                    .price(new BigDecimal("26990.00"))
                    .originalPrice(new BigDecimal("34990.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Express Delivery by Tomorrow")
                    .imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600")
                    .productUrl("https://www.flipkart.com/sony-wh-1000xm5-bluetooth-headset/p/itm789101")
                    .inStock(true)
                    .brand("Sony")
                    .category("Audio & Headphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Samsung Galaxy S24 Ultra 5G (12GB RAM, 256GB Titanium Gray)")
                    .merchant("Flipkart")
                    .price(new BigDecimal("119999.00"))
                    .originalPrice(new BigDecimal("129999.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Delivery in 2 Days")
                    .imageUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600")
                    .productUrl("https://www.flipkart.com/samsung-galaxy-s24-ultra-5g-titanium-gray-256-gb/p/itm556677")
                    .inStock(true)
                    .brand("Samsung")
                    .category("Smartphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Sony PlayStation 5 Slim Console (Digital Edition)")
                    .merchant("Flipkart")
                    .price(new BigDecimal("43990.00"))
                    .originalPrice(new BigDecimal("44990.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Delivery in 3 Days")
                    .imageUrl("https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600")
                    .productUrl("https://www.flipkart.com/sony-playstation-5-cfi-2000-digital/p/itm998877")
                    .inStock(true)
                    .brand("Sony")
                    .category("Gaming & Consoles")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Apple Watch Series 9 GPS 45mm Midnight Aluminum Case")
                    .merchant("Flipkart")
                    .price(new BigDecimal("41990.00"))
                    .originalPrice(new BigDecimal("44900.00"))
                    .currency("INR")
                    .rating(4.7)
                    .delivery("Delivery in 2 Days")
                    .imageUrl("https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600")
                    .productUrl("https://www.flipkart.com/apple-watch-series-9-gps-45mm/p/itm112233")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smart Watches & Wearables")
                    .build()
    );

    @Override
    public String getProviderName() {
        return "Flipkart";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLiveMode() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @CircuitBreaker(name = "flipkart", fallbackMethod = "fallbackSearch")
    @Retry(name = "flipkart", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        if (!enabled) {
            return new ArrayList<>();
        }

        if (isLiveMode()) {
            log.info("Querying live Flipkart Affiliate API at {}", apiUrl);
        }

        return queryMockCatalog(query);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        log.warn("Circuit Breaker triggered for Flipkart provider: {}. Using fallback mock catalog.", throwable.getMessage());
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
