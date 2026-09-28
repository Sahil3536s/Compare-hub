package com.comparehub.service.impl;

import com.comparehub.dto.ProviderStatusDto;
import com.comparehub.provider.ProductProvider;
import com.comparehub.provider.impl.AmazonProductProvider;
import com.comparehub.provider.impl.CromaProductProvider;
import com.comparehub.provider.impl.FlipkartProductProvider;
import com.comparehub.provider.impl.OpenProductDataProvider;
import com.comparehub.service.ProviderMonitoringService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderMonitoringServiceImpl implements ProviderMonitoringService {

    private final List<ProductProvider> providers;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    @Cacheable(value = "provider-health", unless = "#result == null || #result.isEmpty()")
    public List<ProviderStatusDto> getProvidersStatus() {
        List<ProviderStatusDto> statuses = new ArrayList<>();

        for (ProductProvider provider : providers) {
            String name = provider.getProviderName();
            String cbName = getCircuitBreakerName(name);

            CircuitBreaker cb = circuitBreakerRegistry.find(cbName).orElse(null);
            String cbState = cb != null ? cb.getState().name() : "CLOSED";

            boolean isEnabled = true;
            String mode = "LIVE_API";

            if (provider instanceof OpenProductDataProvider) {
                mode = "LIVE_AUTHORIZED_API";
            } else if (provider instanceof AmazonProductProvider amazon) {
                isEnabled = amazon.isEnabled();
                mode = amazon.isLiveMode() ? "LIVE_API" : "MOCK_FALLBACK";
            } else if (provider instanceof FlipkartProductProvider flipkart) {
                isEnabled = flipkart.isEnabled();
                mode = flipkart.isLiveMode() ? "LIVE_API" : "MOCK_FALLBACK";
            } else if (provider instanceof CromaProductProvider croma) {
                isEnabled = croma.isEnabled();
                mode = "MOCK_FALLBACK";
            }

            String overallStatus = !isEnabled ? "DISABLED" : ("OPEN".equals(cbState) ? "DEGRADED" : "UP");

            statuses.add(ProviderStatusDto.builder()
                    .providerName(name)
                    .enabled(isEnabled)
                    .mode(mode)
                    .status(overallStatus)
                    .circuitBreakerState(cbState)
                    .responseTimeMs(50L)
                    .lastError(null)
                    .lastChecked(Instant.now())
                    .build());
        }

        return statuses;
    }

    private String getCircuitBreakerName(String providerName) {
        if ("Amazon".equalsIgnoreCase(providerName)) return "amazon";
        if ("Flipkart".equalsIgnoreCase(providerName)) return "flipkart";
        if ("Croma".equalsIgnoreCase(providerName)) return "croma";
        if ("OpenCommerce".equalsIgnoreCase(providerName)) return "open-products";
        return providerName.toLowerCase();
    }
}
