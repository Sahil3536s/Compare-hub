package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for deserializing the response from the FastAPI ML service.
 * Matches the NEW contract (next‑day prediction).
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

    @JsonProperty("predicted_price")
    private Double predictedPrice;

    @JsonProperty("predicted_change")
    private Double predictedChange;

    @JsonProperty("predicted_change_percent")
    private Double predictedChangePercent;

    @JsonProperty("recommendation")
    private String recommendation;

    @JsonProperty("recommendation_reason")
    private String recommendationReason;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("data_points_used")
    private Integer dataPointsUsed;

    @JsonProperty("message")
    private String message;

}
