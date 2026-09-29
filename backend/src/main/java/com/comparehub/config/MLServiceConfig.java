package com.comparehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the external ML price‑prediction service.
 * The URL is injected from an environment variable or Docker secret.
 */
@Configuration
public class MLServiceConfig {

    /**
     * Base URL of the FastAPI service, e.g. "http://ml-service:8000".
     * Do NOT default to localhost – the environment must provide it.
     */
    @Value("${ml.service.url}")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }
}
