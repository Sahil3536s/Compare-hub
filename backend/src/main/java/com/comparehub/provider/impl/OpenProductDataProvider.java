package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenProductDataProvider implements ProductProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.providers.open-products.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.open-products.api-url:https://dummyjson.com/products/search}")
    private String apiUrl;

    private static final BigDecimal USD_TO_INR_RATE = new BigDecimal("83.50");

    @Override
    public String getProviderName() {
        return "OpenCommerce";
    }

    @Override
    @CircuitBreaker(name = "open-products", fallbackMethod = "fallbackSearch")
    @Retry(name = "open-products", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        if (!enabled) {
            return new ArrayList<>();
        }

        try {
            String searchUrl = apiUrl + (query != null && !query.isBlank() ? "?q=" + query.trim() : "");
            log.info("Querying authorized live product API: {}", searchUrl);

            String responseBody = restClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return fallbackSearch(query, new RuntimeException("Empty response from live API"));
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode productsArray = root.get("products");
            if (productsArray == null || !productsArray.isArray() || productsArray.isEmpty()) {
                return new ArrayList<>();
            }

            List<NormalizedProductOfferDto> results = new ArrayList<>();
            for (JsonNode item : productsArray) {
                String title = item.has("title") ? item.get("title").asText() : "Generic Product";
                String brand = item.has("brand") ? item.get("brand").asText() : "Global";
                String category = item.has("category") ? item.get("category").asText() : "General";
                double rawPriceUsd = item.has("price") ? item.get("price").asDouble() : 0.0;
                double discountPct = item.has("discountPercentage") ? item.get("discountPercentage").asDouble() : 0.0;
                double rating = item.has("rating") ? item.get("rating").asDouble() : 4.5;
                String thumbnail = item.has("thumbnail") ? item.get("thumbnail").asText() : null;

                BigDecimal priceInr = BigDecimal.valueOf(rawPriceUsd)
                        .multiply(USD_TO_INR_RATE)
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal originalPriceInr = priceInr;
                if (discountPct > 0) {
                    originalPriceInr = priceInr.divide(
                            BigDecimal.ONE.subtract(BigDecimal.valueOf(discountPct / 100.0)),
                            2,
                            RoundingMode.HALF_UP
                    );
                }

                results.add(NormalizedProductOfferDto.builder()
                        .productName(title)
                        .merchant("OpenCommerce Live")
                        .price(priceInr)
                        .originalPrice(originalPriceInr)
                        .currency("INR")
                        .rating(rating)
                        .delivery("Authorized Direct Express Delivery")
                        .imageUrl(thumbnail)
                        .productUrl("https://dummyjson.com/products/" + (item.has("id") ? item.get("id").asText() : ""))
                        .inStock(true)
                        .discountPercent((int) Math.round(discountPct))
                        .brand(brand)
                        .category(category)
                        .build());
            }

            log.info("Successfully fetched {} live products from OpenCommerce API", results.size());
            return results;

        } catch (Exception ex) {
            log.warn("Live OpenCommerce API call failed, invoking resilient fallback: {}", ex.getMessage());
            return fallbackSearch(query, ex);
        }
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        log.warn("Executing fallback for OpenCommerce provider. Reason: {}", throwable.getMessage());
        return List.of(
                NormalizedProductOfferDto.builder()
                        .productName("Apple iPhone 15 Pro (128 GB) - Natural Titanium")
                        .merchant("OpenCommerce (Fallback)")
                        .price(new BigDecimal("126999.00"))
                        .originalPrice(new BigDecimal("134900.00"))
                        .currency("INR")
                        .rating(4.8)
                        .delivery("Standard Partner Shipping")
                        .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600")
                        .productUrl("https://example.com/open-products/iphone-15-pro")
                        .inStock(true)
                        .brand("Apple")
                        .category("Smartphones")
                        .build()
        );
    }
}
