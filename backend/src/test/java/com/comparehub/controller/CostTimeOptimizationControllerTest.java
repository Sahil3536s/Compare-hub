package com.comparehub.controller;

import com.comparehub.dto.CostTimeOptionDto;
import com.comparehub.dto.CostTimeOptimizationRequestDto;
import com.comparehub.dto.CostTimeOptimizationResponseDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.CostTimeOptimizationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CostTimeOptimizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CostTimeOptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CostTimeOptimizationService optimizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/optimize/cost-time should return 200 with ranked options and tradeoff summary")
    void testOptimizeEndpoint() throws Exception {
        CostTimeOptionDto opt = CostTimeOptionDto.builder()
                .id("opt_b")
                .title("Option B")
                .cost(BigDecimal.valueOf(5000))
                .durationMinutes(300)
                .compositeScore(85.0)
                .classification("BALANCED")
                .build();

        CostTimeOptimizationResponseDto mockResponse = CostTimeOptimizationResponseDto.builder()
                .costWeight(50.0)
                .timeWeight(50.0)
                .rankedOptions(List.of(opt))
                .topOption(opt)
                .tradeoffSummary("Balanced pick saves transit time.")
                .build();

        when(optimizationService.optimize(any(CostTimeOptimizationRequestDto.class))).thenReturn(mockResponse);

        CostTimeOptimizationRequestDto request = CostTimeOptimizationRequestDto.builder()
                .costWeight(50.0)
                .timeWeight(50.0)
                .options(List.of(opt))
                .build();

        mockMvc.perform(post("/api/optimize/cost-time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costWeight").value(50.0))
                .andExpect(jsonPath("$.topOption.id").value("opt_b"))
                .andExpect(jsonPath("$.topOption.classification").value("BALANCED"));
    }
}
