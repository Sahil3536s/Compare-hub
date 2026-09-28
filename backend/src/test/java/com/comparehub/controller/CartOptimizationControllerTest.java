package com.comparehub.controller;

import com.comparehub.dto.CartItemDto;
import com.comparehub.dto.CartOptimizationRequestDto;
import com.comparehub.dto.CartOptimizationResponseDto;
import com.comparehub.dto.CartPlanDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.CartOptimizationService;
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

@WebMvcTest(CartOptimizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartOptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartOptimizationService cartOptimizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/cart/optimize should return optimization results")
    void testOptimizeCartEndpoint() throws Exception {
        CartPlanDto mockPlan = CartPlanDto.builder()
                .planType("OPTIMIZED_MIXED")
                .grandTotal(new BigDecimal("7300"))
                .totalOrders(2)
                .isRecommended(true)
                .build();

        CartOptimizationResponseDto mockResponse = CartOptimizationResponseDto.builder()
                .strategy("MINIMIZE_PRICE")
                .totalItemsRequested(2)
                .totalItemsMatched(2)
                .estimatedSavings(new BigDecimal("650"))
                .recommendedPlan(mockPlan)
                .explanation("Splitting saves ₹650.")
                .build();

        when(cartOptimizationService.optimizeCart(any())).thenReturn(mockResponse);

        CartOptimizationRequestDto request = CartOptimizationRequestDto.builder()
                .items(List.of(
                        CartItemDto.builder().name("Mouse").quantity(1).build(),
                        CartItemDto.builder().name("Keyboard").quantity(1).build()
                ))
                .strategy("MINIMIZE_PRICE")
                .build();

        mockMvc.perform(post("/api/cart/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("MINIMIZE_PRICE"))
                .andExpect(jsonPath("$.estimatedSavings").value(650))
                .andExpect(jsonPath("$.recommendedPlan.grandTotal").value(7300));
    }
}
