package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO returned by the Spring Boot backend to the React frontend for price prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPricePredictionResponseDto {
    private BigDecimal currentPrice;
    private BigDecimal predictedPrice;
    private Double expectedChangePercent;
    private String recommendation; // BUY_NOW, WAIT, HOLD, INSUFFICIENT_DATA, SERVICE_UNAVAILABLE
    private String errorMessage; // optional error details for the UI
}
