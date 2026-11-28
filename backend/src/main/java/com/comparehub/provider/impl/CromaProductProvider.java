
package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CromaProductProvider implements ProductProvider {

    @Value("${app.providers.croma.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.croma.api-key:}")
    private String apiKey;

    @Value("${app.providers.croma.api-url:https://api.croma.com/v1}")
    private String apiUrl;

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
    public List<NormalizedProductOfferDto> searchProducts(
            String query, int page, int pageSize) {

        if (!enabled) {
            return List.of();
        }

        log.warn(
                "Croma search unavailable: authorized live API integration is not configured. API endpoint: {}",
                apiUrl
        );

        return List.of();
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query, Throwable throwable) {
        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query,
            int page,
            int pageSize,
            Throwable throwable) {

        log.warn(
                "Croma provider failed; refusing to return mock catalogue prices: {}",
                throwable != null ? throwable.getMessage() : "Unknown error"
        );

        return List.of();
    }
}
