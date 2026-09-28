package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardResponseDto {

    private DashboardSummaryDto summary;

    @Builder.Default
    private List<SavedProductResponseDto> recentPriceDrops = new ArrayList<>();

    @Builder.Default
    private List<SavedProductResponseDto> savedProducts = new ArrayList<>();

    @Builder.Default
    private List<PriceAlertResponseDto> activeAlerts = new ArrayList<>();

    @Builder.Default
    private List<RecentActivityDto> recentActivity = new ArrayList<>();

    @Builder.Default
    private List<RecentComparisonDto> recentComparisons = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummaryDto {
        private String userName;
        private String userEmail;
        private long savedProductsCount;
        private long activeAlertsCount;
        private long recentComparisonsCount;
        private long priceDropsCount;
        private BigDecimal potentialSavings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDto {
        private Long id;
        private String title;
        private String description;
        private String activityType;
        private String actionUrl;
        private String actionLabel;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentComparisonDto {
        private Long id;
        private String title;
        private String merchants;
        private String compareUrl;
        private Instant lastCompared;
    }
}
