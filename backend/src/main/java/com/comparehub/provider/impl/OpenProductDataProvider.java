
package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class OpenProductDataProvider implements ProductProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.providers.open-products.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.open-products.api-url:https://dummyjson.com/products/search}")
    private String apiUrl;

    /**
     * Constructor used by unit tests that do not need an HTTP client.
     */
    public OpenProductDataProvider() {
        this.restClient = null;
        this.objectMapper = null;
    }

    /**
     * Constructor used by Spring and acceptance tests.
     */
    @Autowired
    public OpenProductDataProvider(
            RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "OpenCommerce";
    }

    @Override
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    @Override
    @CircuitBreaker(
            name = "open-products",
            fallbackMethod = "fallbackSearch"
    )
    @Retry(
            name = "open-products",
            fallbackMethod = "fallbackSearch"
    )
    public List<NormalizedProductOfferDto> searchProducts(
            String query,
            int page,
            int pageSize) {

        if (!enabled) {
            return List.of();
        }

        log.warn(
                "OpenCommerce search unavailable: no authorized live retailer API integration is configured."
        );

        return List.of();
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query,
            Throwable throwable) {

        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query,
            int page,
            int pageSize,
            Throwable throwable) {

        log.warn(
                "OpenCommerce provider unavailable; refusing to return demo or fabricated prices: {}",
                throwable != null ? throwable.getMessage() : "Unknown error"
        );

        return List.of();
    }
}
