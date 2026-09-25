package com.comparehub.service.impl;

import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.dto.UserDashboardResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.User;
import com.comparehub.repository.PriceAlertRepository;
import com.comparehub.repository.SavedProductRepository;
import com.comparehub.repository.SearchHistoryRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.PriceAlertService;
import com.comparehub.service.SavedProductService;
import com.comparehub.service.SearchHistoryService;
import com.comparehub.service.UserDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDashboardServiceImpl implements UserDashboardService {

    private final UserRepository userRepository;
    private final SavedProductRepository savedProductRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SavedProductService savedProductService;
    private final PriceAlertService priceAlertService;
    private final SearchHistoryService searchHistoryService;

    @Override
    @Transactional(readOnly = true)
    public UserDashboardResponseDto getDashboardData(Long userId) {
        log.info("Generating personalized user dashboard for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Fetch real enriched saved products for current user
        List<SavedProductResponseDto> allSaved = savedProductService.getSavedProductsByUser(userId);

        // 2. Real price drops: only products where currentPrice < savedPrice
        List<SavedProductResponseDto> recentPriceDrops = allSaved.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPriceDropped()))
                .collect(Collectors.toList());

        // 3. Potential Savings: sum of verified price drops on saved items (strictly deterministic)
        BigDecimal potentialSavings = recentPriceDrops.stream()
                .map(p -> p.getPriceDropAmount() != null ? p.getPriceDropAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Counts from PostgreSQL repositories
        long savedProductsCount = savedProductRepository.countByUserId(userId);
        long activeAlertsCount = priceAlertRepository.countByUserIdAndActiveTrue(userId);
        long recentComparisonsCount = searchHistoryRepository.countByUserId(userId);
        long priceDropsCount = recentPriceDrops.size();

        // 5. Active alerts
        List<PriceAlertResponseDto> activeAlerts = priceAlertService.getAlertsByUserId(userId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .limit(6)
                .collect(Collectors.toList());

        // 6. Recent activity timeline & recent comparisons
        List<UserDashboardResponseDto.RecentActivityDto> recentActivity = searchHistoryService.getUserRecentActivity(userId, 8);
        List<UserDashboardResponseDto.RecentComparisonDto> recentComparisons = searchHistoryService.getUserRecentComparisons(userId, 5);

        UserDashboardResponseDto.DashboardSummaryDto summary = UserDashboardResponseDto.DashboardSummaryDto.builder()
                .userName(user.getName())
                .userEmail(user.getEmail())
                .savedProductsCount(savedProductsCount)
                .activeAlertsCount(activeAlertsCount)
                .recentComparisonsCount(recentComparisonsCount)
                .priceDropsCount(priceDropsCount)
                .potentialSavings(potentialSavings)
                .build();

        return UserDashboardResponseDto.builder()
                .summary(summary)
                .recentPriceDrops(recentPriceDrops)
                .savedProducts(allSaved.stream().limit(6).collect(Collectors.toList()))
                .activeAlerts(activeAlerts)
                .recentActivity(recentActivity)
                .recentComparisons(recentComparisons)
                .build();
    }
}
