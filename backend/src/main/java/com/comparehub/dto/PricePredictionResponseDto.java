package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO returned by Spring Boot to the React frontend for ML price prediction.
 * All field names are camelCase (standard JSON for this project).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePredictionResponseDto {

    /**
     * Status of the prediction:
     * SUCCESS, INSUFFICIENT_DATA, ML_UNAVAILABLE, MODEL_NOT_LOADED, ERROR
     */
    private String status;

    private Long productId;
    private String productName;

    /**
     * Current price at time of prediction request.
     */
    private BigDecimal currentPrice;

    /**
     * Estimated price approximately 7 days from now.
     * Only populated when status = SUCCESS.
     * IMPORTANT: This is an estimate, not a guaranteed future price.
     */
    private BigDecimal predictedPrice7d;
    private BigDecimal predictedPrice7Days;

    /** Estimated absolute change from current to predicted price. */
    private BigDecimal predictedChange;

    /** Estimated percentage change. */
    private Double predictedChangePercent;

    /** BUY_NOW, WAIT, or HOLD */
    private String recommendation;

    /** Human-readable reason for the recommendation. */
    private String recommendationReason;

    /** High, Medium, or Low — based on prediction spread. */
    private String confidenceLabel;

    /** Lower bound of the estimated price interval. */
    private BigDecimal predictedPriceLow;
    private BigDecimal predictionRangeLow;

    /** Upper bound of the estimated price interval. */
    private BigDecimal predictedPriceHigh;
    private BigDecimal predictionRangeHigh;

    /**
     * Statistical deal quality: GOOD_DEAL, NORMAL_PRICE, or EXPENSIVE.
     * Computed from rule-based statistical logic, not ML classification.
     */
    private String dealQuality;

    /** Name of the model used for prediction (e.g., RandomForestRegressor). */
    private String modelName;
    private String model;

    /** Model version identifier. */
    private String modelVersion;

    /** Number of real price history observations used. */
    private Integer dataPointsUsed;

    /** Informational message, especially for non-SUCCESS statuses. */
    private String message;
}
