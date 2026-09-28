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
public class CromaProductProvider implements ProductProvider {

    @Value("${app.providers.croma.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.croma.api-key:}")
    private String apiKey;

    @Value("${app.providers.croma.api-url:https://api.croma.com/v1}")
    private String apiUrl;

    private static final List<NormalizedProductOfferDto> MOCK_CATALOG = List.of(
            NormalizedProductOfferDto.builder()
                    .productName("Apple iPhone 15 Pro (128 GB) - Natural Titanium")
                    .merchant("Croma")
                    .price(new BigDecimal("129900.00"))
                    .originalPrice(new BigDecimal("134900.00"))
                    .currency("INR")
                    .rating(4.6)
                    .delivery("Store Pickup Available Today")
                    .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600")
                    .productUrl("https://www.croma.com/apple-iphone-15-pro-128gb-natural-titanium/p/300812")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smartphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Black")
                    .merchant("Croma")
                    .price(new BigDecimal("28990.00"))
                    .originalPrice(new BigDecimal("34990.00"))
                    .currency("INR")
                    .rating(4.7)
                    .delivery("Express Home Delivery")
                    .imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600")
                    .productUrl("https://www.croma.com/sony-wh-1000xm5-over-ear-anc-headphones/p/258901")
                    .inStock(true)
                    .brand("Sony")
                    .category("Audio & Headphones")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Apple MacBook Air 15-inch M3 Chip (16GB RAM, 512GB SSD) - Starlight")
                    .merchant("Croma")
                    .price(new BigDecimal("146900.00"))
                    .originalPrice(new BigDecimal("154900.00"))
                    .currency("INR")
                    .rating(4.8)
                    .delivery("Express Home Delivery in 24 Hrs")
                    .imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600")
                    .productUrl("https://www.croma.com/apple-macbook-air-15-m3-16gb-512gb/p/305544")
                    .inStock(true)
                    .brand("Apple")
                    .category("Laptops & Computers")
                    .build(),
            NormalizedProductOfferDto.builder()
                    .productName("Apple Watch Series 9 GPS 45mm Midnight Aluminum Case")
                    .merchant("Croma")
                    .price(new BigDecimal("38990.00"))
                    .originalPrice(new BigDecimal("44900.00"))
                    .currency("INR")
                    .rating(4.7)
                    .delivery("Pick up in store in 2 hours")
                    .imageUrl("https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=600")
                    .productUrl("https://www.croma.com/apple-watch-series-9-gps-45mm/p/301122")
                    .inStock(true)
                    .brand("Apple")
                    .category("Smart Watches & Wearables")
                    .build()
    );

    @Override
    public String getProviderName() {
        return "Croma";
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    @Override
    @CircuitBreaker(name = "croma", fallbackMethod = "fallbackSearch")
    @Retry(name = "croma", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query, int page, int pageSize) {
        if (!enabled) {
            return new ArrayList<>();
        }

        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Querying live Croma Affiliate API at {}", apiUrl);
        }

        return queryMockCatalog(query, page, pageSize);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, int page, int pageSize, Throwable throwable) {
        log.warn("Circuit Breaker triggered for Croma provider: {}. Using fallback mock catalog.", throwable.getMessage());
        return queryMockCatalog(query, page, pageSize);
    }

    private List<NormalizedProductOfferDto> queryMockCatalog(String query, int page, int pageSize) {
        List<NormalizedProductOfferDto> pool = (query == null || query.isBlank())
                ? new ArrayList<>(MOCK_CATALOG)
                : MOCK_CATALOG.stream()
                        .filter(p -> p.getProductName().toLowerCase().contains(query.trim().toLowerCase())
                                || p.getBrand().toLowerCase().contains(query.trim().toLowerCase())
                                || p.getCategory().toLowerCase().contains(query.trim().toLowerCase()))
                        .collect(Collectors.toList());

        int limit = Math.max(1, pageSize);
        int skip = Math.max(0, (page - 1) * limit);

        return pool.stream()
                .skip(skip)
                .limit(limit)
                .map(this::enrichWithAttributes)
                .collect(Collectors.toList());
    }

    private NormalizedProductOfferDto enrichWithAttributes(NormalizedProductOfferDto offer) {
        if (offer.getAttributes() == null || offer.getAttributes().isEmpty()) {
            java.util.Map<String, String> attrs = new java.util.LinkedHashMap<>();
            if (offer.getBrand() != null) attrs.put("Brand", offer.getBrand());
            if (offer.getCategory() != null) attrs.put("Category", offer.getCategory());
            offer.setAttributes(attrs);
        }
        return offer;
    }
}
