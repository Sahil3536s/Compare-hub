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
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    @Override
    @CircuitBreaker(name = "open-products", fallbackMethod = "fallbackSearch")
    @Retry(name = "open-products", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query, int page, int pageSize) {
        if (!enabled) {
            return new ArrayList<>();
        }

        try {
            int limit = Math.max(1, Math.min(pageSize, 100));
            int skip = Math.max(0, (page - 1) * limit);

            String searchUrl;
            if (query != null && !query.isBlank()) {
                String encodedQ = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
                searchUrl = apiUrl + "?q=" + encodedQ + "&limit=" + limit + "&skip=" + skip;
            } else {
                String base = apiUrl.endsWith("/search") ? apiUrl.substring(0, apiUrl.length() - 7) : apiUrl;
                searchUrl = base + "?limit=" + limit + "&skip=" + skip;
            }

            log.info("Querying authorized live product API: {}", searchUrl);

            String responseBody = restClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return fallbackSearch(query, page, pageSize, new RuntimeException("Empty response from live API"));
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode productsArray = root.get("products");
            if (productsArray == null || !productsArray.isArray() || productsArray.isEmpty()) {
                return new ArrayList<>();
            }

            List<NormalizedProductOfferDto> results = new ArrayList<>();
            for (JsonNode item : productsArray) {
                String title = item.has("title") ? item.get("title").asText() : "Generic Product";
                String brand = item.has("brand") && !item.get("brand").isNull() ? item.get("brand").asText() : "Global";
                String category = item.has("category") ? item.get("category").asText() : "General";
                double rawPriceUsd = item.has("price") ? item.get("price").asDouble() : 0.0;
                double discountPct = item.has("discountPercentage") ? item.get("discountPercentage").asDouble() : 0.0;
                double rating = item.has("rating") ? item.get("rating").asDouble() : 4.5;
                String thumbnail = item.has("thumbnail") ? item.get("thumbnail").asText() : null;
                String externalId = item.has("id") ? item.get("id").asText() : null;

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

                int reviewCount = 0;
                if (item.has("reviews") && item.get("reviews").isArray()) {
                    reviewCount = item.get("reviews").size();
                }

                String shippingInfo = item.has("shippingInformation") && !item.get("shippingInformation").isNull()
                        ? item.get("shippingInformation").asText()
                        : "Authorized Direct Express Delivery";

                boolean inStock = true;
                if (item.has("availabilityStatus")) {
                    String status = item.get("availabilityStatus").asText();
                    inStock = !"Out of Stock".equalsIgnoreCase(status);
                } else if (item.has("stock")) {
                    inStock = item.get("stock").asInt() > 0;
                }

                // Extract generic attributes map
                java.util.Map<String, String> attrMap = new java.util.LinkedHashMap<>();
                if (brand != null && !brand.isBlank()) attrMap.put("Brand", brand);
                if (category != null && !category.isBlank()) attrMap.put("Category", category);
                if (item.has("sku") && !item.get("sku").isNull()) attrMap.put("SKU", item.get("sku").asText());
                if (item.has("warrantyInformation") && !item.get("warrantyInformation").isNull()) {
                    attrMap.put("Warranty", item.get("warrantyInformation").asText());
                }
                if (item.has("shippingInformation") && !item.get("shippingInformation").isNull()) {
                    attrMap.put("Shipping", item.get("shippingInformation").asText());
                }
                if (item.has("returnPolicy") && !item.get("returnPolicy").isNull()) {
                    attrMap.put("Return Policy", item.get("returnPolicy").asText());
                }
                if (item.has("weight") && !item.get("weight").isNull()) {
                    attrMap.put("Weight", item.get("weight").asText() + "g");
                }
                if (item.has("dimensions") && !item.get("dimensions").isNull()) {
                    JsonNode dim = item.get("dimensions");
                    String dims = String.format("%.1fx%.1fx%.1f cm",
                            dim.has("width") ? dim.get("width").asDouble() : 0.0,
                            dim.has("height") ? dim.get("height").asDouble() : 0.0,
                            dim.has("depth") ? dim.get("depth").asDouble() : 0.0);
                    attrMap.put("Dimensions", dims);
                }

                NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                        .productName(title)
                        .title(title)
                        .merchant("OpenCommerce Live")
                        .provider("OpenCommerce Live")
                        .externalProductId(externalId)
                        .price(priceInr)
                        .currentPrice(priceInr)
                        .originalPrice(originalPriceInr)
                        .currency("INR")
                        .rating(rating)
                        .reviewCount(reviewCount)
                        .delivery(shippingInfo)
                        .deliveryEstimate(shippingInfo)
                        .imageUrl(thumbnail)
                        .productUrl("https://dummyjson.com/products/" + (externalId != null ? externalId : ""))
                        .inStock(inStock)
                        .availability(inStock)
                        .discountPercent((int) Math.round(discountPct))
                        .discountPercentage((int) Math.round(discountPct))
                        .brand(brand)
                        .category(category)
                        .attributes(attrMap)
                        .build();

                results.add(offer);
            }

            log.info("Successfully fetched {} live products from OpenCommerce API", results.size());
            return results;

        } catch (Exception ex) {
            log.warn("Live OpenCommerce API call failed, invoking resilient fallback: {}", ex.getMessage());
            return fallbackSearch(query, page, pageSize, ex);
        }
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, int page, int pageSize, Throwable throwable) {
        log.warn("Executing fallback for OpenCommerce provider. Reason: {}", throwable.getMessage());
        if (query != null && query.toLowerCase().contains("iphone")) {
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
        return List.of();
    }
}
