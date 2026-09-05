package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseTimingDto {

    private Integer score; // 0 to 100 (displays as e.g. 8.2 / 10)
    private String status; // "STRONG_BUY_PRICE", "GOOD_TIME_TO_BUY", "NEUTRAL", "CONSIDER_WAITING", "INSUFFICIENT_DATA"
    private String statusLabel; // "Strong Buy Price", "Good Time to Buy", "Fair Market Price", "Consider Waiting", "Insufficient Data"
    private String confidence; // "HIGH", "MEDIUM", "LOW"

    private BigDecimal currentPrice;
    private BigDecimal sevenDayAvg;
    private BigDecimal thirtyDayAvg;
    private BigDecimal ninetyDayLow;
    private String priceTrend; // "DECREASING", "STABLE", "INCREASING"

    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @Builder.Default
    private String disclaimer = "Analysis is based purely on historical pricing trends and does not guarantee future price movements.";
}
