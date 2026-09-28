package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MercadoLibre product provider – fetches live product data from the public MercadoLibre API.
 * This provider supplies generic ecommerce products (including mice, keyboards, etc.) without
 * requiring any commercial credentials. It is enabled by default via {@code app.providers.mercado.enabled}.
 */
@Slf4j
@Component
public class MercadoLibreProductProvider implements ProductProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoLibreProductProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Value("${app.providers.mercado.enabled:true}")
    private boolean enabled;

    // Base URL for MercadoLibre site search (MLA – Argentina). Adjust locale if needed.
    @Value("${app.providers.mercado.api-url:https://api.mercadolibre.com/sites/MLA/search}")
    private String apiUrl;

    @Override
    public String getProviderName() {
        return "MercadoLibre";
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    @Override
    @CircuitBreaker(name = "mercado", fallbackMethod = "fallbackSearch")
    @Retry(name = "mercado", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(String query, int page, int pageSize) {
        if (!enabled) {
            log.info("MercadoLibre provider disabled – returning empty list");
            return new ArrayList<>();
        }
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        try {
            int limit = Math.max(1, Math.min(pageSize, 50));
            int offset = Math.max(0, (page - 1) * limit);
            String url = apiUrl + "?q=" + java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8) + "&limit=" + limit + "&offset=" + offset;
            log.info("Querying MercadoLibre API: {}", url);
            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                log.info("MercadoLibre returned no results for query '{}'", query);
                return new ArrayList<>();
            }
            List<NormalizedProductOfferDto> offers = new ArrayList<>();
            for (JsonNode item : results) {
                String title = item.path("title").asText("Generic Product");
                String seller = item.path("seller").path("nickname").asText("Unknown");
                double priceUsd = item.path("price").asDouble(0.0);
                // MercadoLibre provides price in local currency (ARS). Convert to INR using a static rate for demo.
                BigDecimal priceInr = BigDecimal.valueOf(priceUsd).multiply(BigDecimal.valueOf(0.73)) // rough USD->INR placeholder
                        .setScale(2, RoundingMode.HALF_UP);
                String thumbnail = item.path("thumbnail").asText(null);
                String productUrl = item.path("permalink").asText(null);
                boolean inStock = item.path("available_quantity").asInt(0) > 0;
                // Basic attribute extraction – brand and category are often embedded in title or tags.
                Map<String, String> attrs = extractAttributes(item);
                NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                        .productName(title)
                        .title(title)
                        .merchant(seller)
                        .provider("MercadoLibre")
                        .price(priceInr)
                        .currentPrice(priceInr)
                        .currency("INR")
                        .inStock(inStock)
                        .imageUrl(thumbnail)
                        .productUrl(productUrl)
                        .attributes(attrs)
                        .build();
                offers.add(offer);
            }
            log.info("MercadoLibre fetched {} offers for query '{}'", offers.size(), query);
            return offers;
        } catch (Exception ex) {
            log.warn("Error querying MercadoLibre API: {} – invoking fallback", ex.getMessage());
            return fallbackSearch(query, page, pageSize, ex);
        }
    }

    private Map<String, String> extractAttributes(JsonNode item) {
        // Simple attribute extraction – look for brand, category, and condition if present.
        // This can be extended in the future.
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        if (item.hasNonNull("category_id")) {
            map.put("Category", item.get("category_id").asText());
        }
        if (item.hasNonNull("domain_id")) {
            map.put("Domain", item.get("domain_id").asText());
        }
        if (item.hasNonNull("condition")) {
            map.put("Condition", item.get("condition").asText());
        }
        return map;
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, Throwable throwable) {
        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(String query, int page, int pageSize, Throwable throwable) {
        log.warn("MercadoLibre fallback triggered for query '{}': {}", query, throwable.getMessage());
        // Return an empty list – we avoid hard‑coding demo data to respect user constraints.
        return new ArrayList<>();
    }
}
