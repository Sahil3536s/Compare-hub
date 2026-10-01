package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned by Spring Boot to the React frontend for ML price prediction.
 * Fields align with the NEW FastAPI next‑day prediction contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricePredictionResponseDto {

    /** Status of the prediction: SUCCESS, INSUFFICIENT_DATA, ML_UNAVAILABLE, MODEL_NOT_LOADED, ERROR */
    private String status;

    private Long productId;
    private String productName;

    /** Current price at time of request */
    private java.math.BigDecimal currentPrice;

    /** Predicted price for the next day */
    private java.math.BigDecimal predictedPrice;

    /** Absolute change from current price to predicted next‑day price */
    private java.math.BigDecimal predictedChange;

    /** Percentage change (e.g., 1.06) */
    private Double predictedChangePercent;

    /** BUY_NOW, WAIT, or HOLD */
    private String recommendation;

    /** Human‑readable reason for the recommendation */
    private String recommendationReason;

    /** Deterministic deal quality (statistical, not ML) */
    private String dealQuality;

    /** Model metadata */
    private String modelName;
    private String modelVersion;

    /** Number of real price‑history observations used */
    private Integer dataPointsUsed;

    /** Optional informational message (e.g., insufficient data) */
    private String message;
}
