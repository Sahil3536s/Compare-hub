package com.comparehub.client;

import com.comparehub.dto.MLServiceResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the FastAPI ML service.
 *
 * <p>Uses Spring Boot's RestClient with:
 * - Configurable base URL (ML_SERVICE_URL environment variable)
 * - 5s connect timeout, 10s read timeout
 * - Resilience4j circuit breaker (instance: "mlService")
 *   → If the ML service is down, the circuit opens and the fallback returns Optional.empty()
 *   → Normal comparison features are completely unaffected
 */
@Slf4j
@Component
public class MLServiceClient {

    private final RestClient restClient;

    public MLServiceClient(
            @Value("${app.ml.service-url:http://localhost:8000}") String mlServiceUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("[ML] MLServiceClient configured with base URL: {}", mlServiceUrl);
    }

    /**
     * Call the ML service /predict-price endpoint.
     *
     * @param productId   the product ID
     * @param pricePoints list of {date, price, merchant} maps in chronological order
     * @return Optional containing the ML response, or empty if the ML service is unavailable
     */
    @CircuitBreaker(name = "mlService", fallbackMethod = "predictFallback")
    public Optional<MLServiceResponseDto> predict(Long productId, List<Map<String, Object>> pricePoints) {
        log.debug("[ML] Calling predict-price for product {} with {} points", productId, pricePoints.size());

        var requestBody = Map.of(
                "product_id", (Object) productId,
                "price_points", pricePoints
        );

        var response = restClient.post()
                .uri("/predict-price")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(MLServiceResponseDto.class);

        log.debug("[ML] Received prediction status: {}", response != null ? response.getStatus() : "null");
        return Optional.ofNullable(response);
    }

    /**
     * Fallback method invoked by Resilience4j when the circuit is open or an exception occurs.
     * Returns Optional.empty() so the caller can return ML_UNAVAILABLE gracefully.
     */
    public Optional<MLServiceResponseDto> predictFallback(
            Long productId, List<Map<String, Object>> pricePoints, Exception ex) {
        log.warn("[ML] Circuit breaker fallback triggered for product {} — reason: {}",
                productId, ex.getMessage());
        return Optional.empty();
    }
}
