package com.comparehub.controller;

import com.comparehub.dto.SavingsEventDto;
import com.comparehub.dto.SavingsSummaryDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.UserSavingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserSavingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserSavingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserSavingsService userSavingsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/savings/dashboard should return savings summary metrics")
    void testGetSavingsDashboard() throws Exception {
        SavingsSummaryDto summary = SavingsSummaryDto.builder()
                .thisMonthPotentialSavings(BigDecimal.valueOf(4280))
                .thisMonthConfirmedSavings(BigDecimal.valueOf(7200))
                .totalComparisons(37)
                .dealsFoundCount(12)
                .priceAlertsCount(4)
                .largestSaving(SavingsSummaryDto.LargestSavingDto.builder()
                        .title("Laptop")
                        .amount(BigDecimal.valueOf(7200))
                        .build())
                .build();

        when(userSavingsService.getSavingsSummary(any())).thenReturn(summary);

        mockMvc.perform(get("/api/savings/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thisMonthPotentialSavings").value(4280))
                .andExpect(jsonPath("$.totalComparisons").value(37))
                .andExpect(jsonPath("$.largestSaving.title").value("Laptop"))
                .andExpect(jsonPath("$.largestSaving.amount").value(7200));
    }

    @Test
    @DisplayName("POST /api/savings/events/{id}/confirm should confirm potential saving")
    void testConfirmSavingsEvent() throws Exception {
        SavingsEventDto confirmed = SavingsEventDto.builder()
                .id(1L)
                .title("Smartphone")
                .eventType("CONFIRMED")
                .savingAmount(BigDecimal.valueOf(4280))
                .build();

        when(userSavingsService.confirmSavings(eq(1L))).thenReturn(confirmed);

        mockMvc.perform(post("/api/savings/events/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventType").value("CONFIRMED"));
    }
}
