package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatusDto {

    private String providerName;
    private Boolean enabled;
    private String mode; // "LIVE_API" | "MOCK_FALLBACK"
    private String status; // "UP" | "DEGRADED" | "DISABLED"
    private String circuitBreakerState; // "CLOSED" | "OPEN" | "HALF_OPEN"
    private Long responseTimeMs;
    private String lastError;
    private Instant lastChecked;
}
