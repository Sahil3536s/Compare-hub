package com.comparehub.service.impl;

import com.comparehub.config.MLServiceConfig;
import com.comparehub.dto.MLPricePredictionResponseDto;
import com.comparehub.model.Product;
import com.comparehub.service.MLPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Communicates with the external FastAPI ML service.
 * Gracefully handles service failures and timeouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLPredictionServiceImpl implements MLPredictionService {

    private final MLServiceConfig mlServiceConfig;

    /**
     * Creates a RestTemplate with short connection/read timeouts
     * so ML service failures do not block the main application.
     */
    private RestTemplate restTemplate() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                (int) Duration.ofSeconds(3).toMillis()
        );

        factory.setReadTimeout(
                (int) Duration.ofSeconds(3).toMillis()
        );

        return new RestTemplate(factory);
    }

    @Override
    public boolean isHealthy() {

        String healthUrl = UriComponentsBuilder
                .fromHttpUrl(mlServiceConfig.getBaseUrl())
                .pathSegment("health")
                .toUriString();

        try {

            ResponseEntity<String> response =
                    restTemplate().getForEntity(
                            healthUrl,
                            String.class
                    );

            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException e) {

            log.warn(
                    "ML service health check failed: {}",
                    e.getMessage()
            );

            return false;
        }
    }

    @Override
    public MLPricePredictionResponseDto predict(Product product) {

        String predictUrl = UriComponentsBuilder
                .fromHttpUrl(mlServiceConfig.getBaseUrl())
                .pathSegment("predict")
                .toUriString();

        BigDecimal currentPrice = BigDecimal.ZERO;

        if (product.getOffers() != null
                && !product.getOffers().isEmpty()
                && product.getOffers().get(0).getPrice() != null) {

            currentPrice =
                    product.getOffers().get(0).getPrice();
        }

        /*
         * Temporary integration payload.
         * Replace/extend history once the ML team finalizes
         * the FastAPI request schema.
         */
        Map<String, Object> requestBody = Map.of(
                "productId", product.getId(),
                "currentPrice", currentPrice,
                "history", Collections.emptyList()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {

            ResponseEntity<Map> response =
                    restTemplate().exchange(
                            predictUrl,
                            HttpMethod.POST,
                            request,
                            Map.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return buildUnavailable(
                        response.getStatusCode()
                );
            }

            Map body = response.getBody();

            if (body == null
                    || !body.containsKey("predictedPrice")) {

                return buildInsufficientData(
                        "Invalid response structure"
                );
            }

            BigDecimal returnedCurrentPrice =
                    new BigDecimal(
                            body.getOrDefault(
                                    "currentPrice",
                                    "0"
                            ).toString()
                    );

            BigDecimal predictedPrice =
                    new BigDecimal(
                            body.get("predictedPrice").toString()
                    );

            Double expectedChangePercent =
                    Double.valueOf(
                            body.getOrDefault(
                                    "expectedChangePercent",
                                    "0"
                            ).toString()
                    );

            String recommendation =
                    body.getOrDefault(
                            "recommendation",
                            "INSUFFICIENT_DATA"
                    ).toString();

            return MLPricePredictionResponseDto.builder()
                    .currentPrice(returnedCurrentPrice)
                    .predictedPrice(predictedPrice)
                    .expectedChangePercent(expectedChangePercent)
                    .recommendation(recommendation)
                    .build();

        } catch (HttpStatusCodeException e) {

            log.error(
                    "ML service returned error status {}: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            return buildUnavailable(
                    e.getStatusCode()
            );

        } catch (RestClientException e) {

            log.error(
                    "Error communicating with ML service: {}",
                    e.getMessage()
            );

            return buildUnavailable(null);
        }
    }

    private MLPricePredictionResponseDto buildUnavailable(
            HttpStatusCode status) {

        return MLPricePredictionResponseDto.builder()
                .recommendation("SERVICE_UNAVAILABLE")
                .errorMessage(
                        status != null
                                ? "ML service error: " + status
                                : "ML service unreachable"
                )
                .build();
    }

    private MLPricePredictionResponseDto buildInsufficientData(
            String message) {

        return MLPricePredictionResponseDto.builder()
                .recommendation("INSUFFICIENT_DATA")
                .errorMessage(message)
                .build();
    }
}