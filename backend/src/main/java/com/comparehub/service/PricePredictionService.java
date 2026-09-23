package com.comparehub.service;

import com.comparehub.dto.PricePredictionResponseDto;

/**
 * Service interface for ML-powered price predictions.
 * Retrieves historical price data, validates it, and delegates to the ML service.
 */
public interface PricePredictionService {

    /**
     * Get an ML price prediction for the given product.
     *
     * @param productId the product to predict for
     * @return PricePredictionResponseDto with status SUCCESS, INSUFFICIENT_DATA,
     *         ML_UNAVAILABLE, or ERROR
     * @throws com.comparehub.exception.ResourceNotFoundException if product not found
     */
    PricePredictionResponseDto getPricePrediction(Long productId);
}
