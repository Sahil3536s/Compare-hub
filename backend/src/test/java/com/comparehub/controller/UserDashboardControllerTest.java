package com.comparehub.controller;

import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.dto.UserDashboardResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.UserDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDashboardService userDashboardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "Alex", "alex@comparehub.test", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/dashboard returns 200 with complete dashboard payload")
    void shouldReturnDashboardDataForAuthenticatedUser() throws Exception {
        UserDashboardResponseDto.DashboardSummaryDto summary = UserDashboardResponseDto.DashboardSummaryDto.builder()
                .userName("Alex")
                .userEmail("alex@comparehub.test")
                .savedProductsCount(4L)
                .activeAlertsCount(2L)
                .recentComparisonsCount(7L)
                .priceDropsCount(1L)
                .potentialSavings(new BigDecimal("2500.00"))
                .build();

        SavedProductResponseDto dropItem = SavedProductResponseDto.builder()
                .id(10L)
                .productName("Samsung Galaxy S24")
                .savedPrice(new BigDecimal("59999.00"))
                .currentPrice(new BigDecimal("57499.00"))
                .isPriceDropped(true)
                .priceDropAmount(new BigDecimal("2500.00"))
                .priceDropPercentage(4.2)
                .currentMerchant("Flipkart")
                .build();

        UserDashboardResponseDto dashboard = UserDashboardResponseDto.builder()
                .summary(summary)
                .recentPriceDrops(List.of(dropItem))
                .savedProducts(List.of(dropItem))
                .activeAlerts(List.of(PriceAlertResponseDto.builder().id(20L).productName("Samsung Galaxy S24").build()))
                .recentActivity(List.of(UserDashboardResponseDto.RecentActivityDto.builder().title("Compared Samsung Galaxy S24").build()))
                .recentComparisons(List.of(UserDashboardResponseDto.RecentComparisonDto.builder().title("Samsung Galaxy S24").build()))
                .build();

        when(userDashboardService.getDashboardData(1L)).thenReturn(dashboard);

        mockMvc.perform(get("/api/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.userName").value("Alex"))
                .andExpect(jsonPath("$.summary.savedProductsCount").value(4))
                .andExpect(jsonPath("$.summary.activeAlertsCount").value(2))
                .andExpect(jsonPath("$.summary.recentComparisonsCount").value(7))
                .andExpect(jsonPath("$.summary.priceDropsCount").value(1))
                .andExpect(jsonPath("$.summary.potentialSavings").value(2500.00))
                .andExpect(jsonPath("$.recentPriceDrops[0].productName").value("Samsung Galaxy S24"))
                .andExpect(jsonPath("$.recentPriceDrops[0].isPriceDropped").value(true))
                .andExpect(jsonPath("$.recentPriceDrops[0].priceDropAmount").value(2500.00));
    }
}
