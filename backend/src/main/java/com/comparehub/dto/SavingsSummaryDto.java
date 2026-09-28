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
public class SavingsSummaryDto {

    private BigDecimal totalPotentialSavings;
    private BigDecimal totalConfirmedSavings;
    private BigDecimal thisMonthPotentialSavings;
    private BigDecimal thisMonthConfirmedSavings;

    private int totalComparisons;
    private int thisMonthComparisons;
    private int dealsFoundCount;
    private int priceAlertsCount;
    private int triggeredAlertsCount;
    private int savedProductsCount;

    private LargestSavingDto largestSaving;

    @Builder.Default
    private List<MonthlySavingsDto> monthlyHistory = new ArrayList<>();

    @Builder.Default
    private List<CategorySavingsDto> categoryBreakdowns = new ArrayList<>();

    private PriceAlertSuccessDto priceAlertSuccess;

    @Builder.Default
    private List<SavingsEventDto> recentEvents = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargestSavingDto {
        private String title;
        private String category;
        private BigDecimal amount;
        private String merchant;
        private String comparisonContext;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySavingsDto {
        private String month;
        private BigDecimal potentialAmount;
        private BigDecimal confirmedAmount;
        private int comparisonsCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySavingsDto {
        private String category;
        private BigDecimal savingAmount;
        private double percentage;
        private int comparisonCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAlertSuccessDto {
        private int totalAlerts;
        private int triggeredAlerts;
        private double successRate;
        private BigDecimal averageDropAmount;
    }
}
