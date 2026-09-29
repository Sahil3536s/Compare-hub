package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceMeterDto {

    /**
     * Rating classifications:
     * EXCELLENT_DEAL, GOOD_PRICE, AVERAGE_PRICE, ABOVE_AVERAGE, HIGH_PRICE, INSUFFICIENT_DATA
     */
    private String classification;
    private String classificationLabel; // e.g. "Excellent Deal", "Good Price", "Average Price", "Above Average", "High Price"

    private BigDecimal currentPrice;
    private BigDecimal historicalMinimum;
    private BigDecimal historicalMaximum;
    private BigDecimal historicalAverage;
    private BigDecimal historicalMedian;

    private Double percentDifferenceFromAverage; // negative = below average, positive = above average
    private Double relativePositionWithinHistoricalRange; // 0.0 (at min) to 1.0 (at max)
    private Integer score; // 0 (poor buying time) to 100 (good buying time)
    private String summaryText; // e.g. "Current price is 7.1% lower than its 90-day average."
    private String period; // e.g. "90D"
    private Integer observationsCount;
    private Boolean hasSufficientData;

    public BigDecimal getHistoricalLowest() {
        return historicalMinimum;
    }

    public BigDecimal getHistoricalHighest() {
        return historicalMaximum;
    }

    public Double getDifferencePercentage() {
        return percentDifferenceFromAverage;
    }

    public BigDecimal getDifferenceFromAvg() {
        return (historicalAverage != null && currentPrice != null)
                ? currentPrice.subtract(historicalAverage)
                : null;
    }

    public String getAdvice() {
        return summaryText;
    }

    public String getStatus() {
        return Boolean.TRUE.equals(hasSufficientData) ? "CALCULATED" : "INSUFFICIENT_DATA";
    }
}
