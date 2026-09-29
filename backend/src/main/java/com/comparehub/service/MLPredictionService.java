package com.comparehub.service;

import com.comparehub.dto.MLPricePredictionResponseDto;
import com.comparehub.model.Product;

/**
 * Service that contacts the external FastAPI ML price‑prediction service.
 */
public interface MLPredictionService {
    /**
     * Returns the health status of the ML service.
     * @return true if the service responded with HTTP 200 to /health, false otherwise
     */
    boolean isHealthy();

    /**
     * Calls the ML service to obtain a prediction for the given product.
     * Handles all error scenarios and translates them into a structured DTO.
     *
     * @param product the product for which to predict price (must contain current price and ID)
     * @return DTO containing prediction data or error information
     */
    MLPricePredictionResponseDto predict(Product product);
}
