package com.comparehub.controller;

import com.comparehub.dto.PaymentPreferenceDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.PaymentOfferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentOfferService paymentOfferService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/preferences/payment should return default or user payment preferences")
    void testGetPreferences() throws Exception {
        PaymentPreferenceDto mockPrefs = PaymentPreferenceDto.builder()
                .preferredBank("HDFC")
                .preferredCardType("CREDIT_CARD")
                .hasUpi(true)
                .hasWallet(false)
                .preferredWallet("NONE")
                .build();

        when(paymentOfferService.getUserPreferences(any())).thenReturn(mockPrefs);

        mockMvc.perform(get("/api/preferences/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredBank").value("HDFC"))
                .andExpect(jsonPath("$.preferredCardType").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.hasUpi").value(true));
    }
}
