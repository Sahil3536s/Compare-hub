package com.comparehub.controller;

import com.comparehub.dto.PricePredictionResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.service.PricePredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ML price prediction.
 *
 * <p>Endpoint: GET /api/products/{productId}/prediction
 *
 * <p>Authentication: Public (same as the existing price-history endpoint).
 * The ML service is reached via PricePredictionService → MLServiceClient.
 * React MUST NOT call the ML service directly — all calls are proxied here.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PricePredictionController {

    private final PricePredictionService pricePredictionService;

    /**
     * Get an ML-powered price prediction for a product.
     *
     * <p>Returns 200 in all cases (even when ML is unavailable or data is insufficient),
     * with the {@code status} field indicating the outcome:
     * <ul>
     *   <li>SUCCESS — prediction available</li>
     *   <li>INSUFFICIENT_DATA — not enough price history yet</li>
     *   <li>ML_UNAVAILABLE — ML service is down (comparison features still work)</li>
     *   <li>MODEL_NOT_LOADED — model not yet trained</li>
     *   <li>ERROR — unexpected error</li>
     * </ul>
     *
     * @param productId the product to predict for
     * @return 200 with prediction DTO, or 404 if product not found
     */
    @GetMapping("/{productId}/prediction")
    public ResponseEntity<PricePredictionResponseDto> getPricePrediction(
            @PathVariable Long productId) {

        log.debug("Price prediction requested for product {}", productId);

        try {
            PricePredictionResponseDto response = pricePredictionService.getPricePrediction(productId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            throw ex; // Handled by GlobalExceptionHandler — returns 404
        } catch (Exception ex) {
            log.error("Unexpected error during price prediction for product {}: {}", productId, ex.getMessage(), ex);
            return ResponseEntity.ok(
                    PricePredictionResponseDto.builder()
                            .status("ERROR")
                            .productId(productId)
                            .message("An unexpected error occurred while generating the price prediction.")
                            .build()
            );
        }
    }
}
