package com.comparehub.service;

import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.dto.UserDashboardResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.User;
import com.comparehub.repository.PriceAlertRepository;
import com.comparehub.repository.SavedProductRepository;
import com.comparehub.repository.SearchHistoryRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.UserDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SavedProductRepository savedProductRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private SavedProductService savedProductService;

    @Mock
    private PriceAlertService priceAlertService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private UserDashboardServiceImpl userDashboardService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Alex Smith")
                .email("alex@comparehub.test")
                .build();
    }

    @Test
    @DisplayName("Should assemble dashboard with real counts, potential savings, and price drops")
    void testGetDashboardData_realDataCalculation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SavedProductResponseDto itemWithDrop = SavedProductResponseDto.builder()
                .id(101L)
                .productId(501L)
                .productName("Samsung Galaxy S24")
                .savedPrice(new BigDecimal("60000.00"))
                .currentPrice(new BigDecimal("56500.00"))
                .isPriceDropped(true)
                .priceDropAmount(new BigDecimal("3500.00"))
                .priceDropPercentage(5.8)
                .currentMerchant("Flipkart")
                .build();

        SavedProductResponseDto itemNoDrop = SavedProductResponseDto.builder()
                .id(102L)
                .productId(502L)
                .productName("Sony WH-1000XM5")
                .savedPrice(new BigDecimal("26990.00"))
                .currentPrice(new BigDecimal("27990.00"))
                .isPriceDropped(false)
                .priceDropAmount(BigDecimal.ZERO)
                .currentMerchant("Amazon")
                .build();

        when(savedProductService.getSavedProductsByUser(1L))
                .thenReturn(List.of(itemWithDrop, itemNoDrop));

        when(savedProductRepository.countByUserId(1L)).thenReturn(2L);
        when(priceAlertRepository.countByUserIdAndActiveTrue(1L)).thenReturn(1L);
        when(searchHistoryRepository.countByUserId(1L)).thenReturn(8L);

        PriceAlertResponseDto alert = PriceAlertResponseDto.builder()
                .id(201L)
                .productId(501L)
                .productName("Samsung Galaxy S24")
                .targetPrice(new BigDecimal("55000.00"))
                .active(true)
                .build();

        when(priceAlertService.getAlertsByUserId(1L)).thenReturn(List.of(alert));

        UserDashboardResponseDto.RecentActivityDto act1 = UserDashboardResponseDto.RecentActivityDto.builder()
                .id(301L)
                .title("Compared Samsung Galaxy S24")
                .description("Amazon vs Flipkart vs Croma")
                .activityType("PRODUCT_COMPARE")
                .actionUrl("/shopping?q=Samsung+Galaxy+S24")
                .actionLabel("Compare Again")
                .createdAt(Instant.now())
                .build();

        when(searchHistoryService.getUserRecentActivity(1L, 8)).thenReturn(List.of(act1));

        UserDashboardResponseDto.RecentComparisonDto comp1 = UserDashboardResponseDto.RecentComparisonDto.builder()
                .id(401L)
                .title("Samsung Galaxy S24")
                .merchants("Amazon vs Flipkart vs Croma")
                .compareUrl("/shopping?q=Samsung+Galaxy+S24")
                .lastCompared(Instant.now())
                .build();

        when(searchHistoryService.getUserRecentComparisons(1L, 5)).thenReturn(List.of(comp1));

        UserDashboardResponseDto response = userDashboardService.getDashboardData(1L);

        assertThat(response).isNotNull();
        assertThat(response.getSummary()).isNotNull();

        // Verify Summary statistics
        assertThat(response.getSummary().getUserName()).isEqualTo("Alex Smith");
        assertThat(response.getSummary().getSavedProductsCount()).isEqualTo(2L);
        assertThat(response.getSummary().getActiveAlertsCount()).isEqualTo(1L);
        assertThat(response.getSummary().getRecentComparisonsCount()).isEqualTo(8L);
        assertThat(response.getSummary().getPriceDropsCount()).isEqualTo(1L);
        assertThat(response.getSummary().getPotentialSavings()).isEqualByComparingTo(new BigDecimal("3500.00"));

        // Verify Recent Price Drops (only itemWithDrop)
        assertThat(response.getRecentPriceDrops()).hasSize(1);
        assertThat(response.getRecentPriceDrops().get(0).getProductName()).isEqualTo("Samsung Galaxy S24");
        assertThat(response.getRecentPriceDrops().get(0).getIsPriceDropped()).isTrue();

        // Verify Saved items
        assertThat(response.getSavedProducts()).hasSize(2);

        // Verify Active Alerts
        assertThat(response.getActiveAlerts()).hasSize(1);

        // Verify Recent Activity
        assertThat(response.getRecentActivity()).hasSize(1);
        assertThat(response.getRecentActivity().get(0).getActionLabel()).isEqualTo("Compare Again");
    }

    @Test
    @DisplayName("Zero potential savings when no prices have dropped (No fabrication)")
    void testGetDashboardData_zeroSavingsWhenNoDrop() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SavedProductResponseDto itemNoDrop = SavedProductResponseDto.builder()
                .id(102L)
                .productId(502L)
                .productName("Sony WH-1000XM5")
                .savedPrice(new BigDecimal("26990.00"))
                .currentPrice(new BigDecimal("27990.00"))
                .isPriceDropped(false)
                .priceDropAmount(BigDecimal.ZERO)
                .build();

        when(savedProductService.getSavedProductsByUser(1L)).thenReturn(List.of(itemNoDrop));
        when(savedProductRepository.countByUserId(1L)).thenReturn(1L);
        when(priceAlertRepository.countByUserIdAndActiveTrue(1L)).thenReturn(0L);
        when(searchHistoryRepository.countByUserId(1L)).thenReturn(0L);
        when(priceAlertService.getAlertsByUserId(1L)).thenReturn(List.of());
        when(searchHistoryService.getUserRecentActivity(1L, 8)).thenReturn(List.of());
        when(searchHistoryService.getUserRecentComparisons(1L, 5)).thenReturn(List.of());

        UserDashboardResponseDto response = userDashboardService.getDashboardData(1L);

        assertThat(response.getSummary().getPriceDropsCount()).isEqualTo(0L);
        assertThat(response.getSummary().getPotentialSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getRecentPriceDrops()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown userId")
    void testGetDashboardData_userNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDashboardService.getDashboardData(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
