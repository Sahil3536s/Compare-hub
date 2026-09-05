package com.comparehub.controller;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.ComparisonRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComparisonRecommendationService recommendationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnProductRecommendation() throws Exception {
        AiRecommendationDto dto = AiRecommendationDto.builder()
                .bestOverall("Amazon")
                .cheapest("Flipkart")
                .recommendation("Amazon costs ₹500 more but has higher rating.")
                .reasoningPoints(List.of("Higher customer rating", "Faster delivery"))
                .build();

        when(recommendationService.recommendProducts(anyList())).thenReturn(dto);

        mockMvc.perform(post("/api/recommendations/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestOverall").value("Amazon"))
                .andExpect(jsonPath("$.cheapest").value("Flipkart"))
                .andExpect(jsonPath("$.recommendation").value("Amazon costs ₹500 more but has higher rating."));
    }
}
