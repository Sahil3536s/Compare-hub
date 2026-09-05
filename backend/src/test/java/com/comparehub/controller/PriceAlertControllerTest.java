package com.comparehub.controller;

import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.PriceAlertService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceAlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class PriceAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceAlertService priceAlertService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private void authenticateUser() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "John Doe",
                "john@example.com",
                "password",
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void shouldReturnUserAlerts() throws Exception {
        authenticateUser();

        PriceAlertResponseDto alert = PriceAlertResponseDto.builder()
                .id(50L)
                .productName("iPhone 15")
                .targetPrice(new BigDecimal("75000.00"))
                .active(true)
                .build();

        when(priceAlertService.getAlertsByUserId(1L)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].productName").value("iPhone 15"));
    }

    @Test
    void shouldCreateAlertSuccessfully() throws Exception {
        authenticateUser();

        PriceAlertResponseDto created = PriceAlertResponseDto.builder()
                .id(51L)
                .productName("Sony WH-1000XM5")
                .targetPrice(new BigDecimal("22000.00"))
                .active(true)
                .build();

        when(priceAlertService.createPriceAlert(any())).thenReturn(created);

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 20, \"targetPrice\": 22000.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.productName").value("Sony WH-1000XM5"));
    }
}
