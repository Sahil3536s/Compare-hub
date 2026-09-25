package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Strongly typed DTO for sending historical price observations to the FastAPI ML service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequestDto {

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("price_points")
    private List<PricePointDto> pricePoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricePointDto {
        private String date;
        private Double price;
        private String merchant;
    }
}
