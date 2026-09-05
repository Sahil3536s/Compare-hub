package com.comparehub.controller;

import com.comparehub.dto.ProductRankingWeightsDto;
import com.comparehub.dto.UserRankingPreferenceDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.PersonalizedRankingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class RankingPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonalizedRankingService personalizedRankingService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/ranking/preferences should return default or user ranking preferences")
    void testGetPreferences() throws Exception {
        UserRankingPreferenceDto mockPrefs = UserRankingPreferenceDto.builder()
                .preset("BALANCED")
                .product(ProductRankingWeightsDto.builder()
                        .price(40)
                        .rating(20)
                        .discount(15)
                        .delivery(15)
                        .reliability(10)
                        .build())
                .build();

        when(personalizedRankingService.getUserPreferences(any())).thenReturn(mockPrefs);

        mockMvc.perform(get("/api/ranking/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preset").value("BALANCED"))
                .andExpect(jsonPath("$.product.price").value(40))
                .andExpect(jsonPath("$.product.rating").value(20));
    }
}
