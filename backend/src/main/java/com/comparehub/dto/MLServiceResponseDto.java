package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for deserializing the raw JSON response from the FastAPI ML service.
 * Field names match the snake_case JSON produced by the Python service.
 * This is NOT exposed to the frontend — use PricePredictionResponseDto for that.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLServiceResponseDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("current_price")
    private Double currentPrice;

    @JsonProperty("predicted_price_7d")
    private Double predictedPrice7d;

    @JsonProperty("predicted_change")
    private Double predictedChange;

    @JsonProperty("predicted_change_percent")
    private Double predictedChangePercent;

    @JsonProperty("recommendation")
    private String recommendation;

    @JsonProperty("recommendation_reason")
    private String recommendationReason;

    @JsonProperty("confidence_label")
    private String confidenceLabel;

    @JsonProperty("predicted_price_low")
    private Double predictedPriceLow;

    @JsonProperty("predicted_price_high")
    private Double predictedPriceHigh;

    @JsonProperty("deal_quality")
    private String dealQuality;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("data_points_used")
    private Integer dataPointsUsed;

    @JsonProperty("message")
    private String message;
}
