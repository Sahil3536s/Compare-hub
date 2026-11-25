package com.comparehub.provider.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.provider.ProductProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FlipkartProductProvider implements ProductProvider {

    @Value("${app.providers.flipkart.enabled:true}")
    private boolean enabled;

    @Value("${app.providers.flipkart.api-key:}")
    private String apiKey;

    @Value("${app.providers.flipkart.api-url:https://affiliate-api.flipkart.net/v1}")
    private String apiUrl;

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
    public List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    @Override
    @CircuitBreaker(name = "flipkart", fallbackMethod = "fallbackSearch")
    @Retry(name = "flipkart", fallbackMethod = "fallbackSearch")
    public List<NormalizedProductOfferDto> searchProducts(
            String query,
            int page,
            int pageSize
    ) {
        if (!enabled) {
            return new ArrayList<>();
        }

        // Do not present hardcoded catalogue entries as current marketplace offers.
        // Real marketplace API integration requires authorized credentials
        // and an implemented API client.
        log.warn(
                "Flipkart live product search is unavailable: "
                        + "authorized API integration is not configured."
        );

        return List.of();
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query,
            Throwable throwable
    ) {
        return fallbackSearch(query, 1, 20, throwable);
    }

    public List<NormalizedProductOfferDto> fallbackSearch(
            String query,
            int page,
            int pageSize,
            Throwable throwable
    ) {
        log.warn(
                "Flipkart provider failed; refusing to return stale mock prices: {}",
                throwable.getMessage()
        );

        return List.of();
    }
}