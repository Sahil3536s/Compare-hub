package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Typed request DTO sent by Spring Boot to the FastAPI ML service POST /predict endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionRequestDto {

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("price_points")
    private List<MLPricePointDto> pricePoints;
}
