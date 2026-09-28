package com.comparehub.controller;

import com.comparehub.dto.DecisionRecommendation;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.DecisionAdvisorService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionAdvisorController.class)
@AutoConfigureMockMvc(addFilters = false)
class DecisionAdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DecisionAdvisorService decisionAdvisorService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/advisor/evaluate should return decision recommendation")
    void testEvaluateDecision() throws Exception {
        DecisionRecommendation mockRecommendation = DecisionRecommendation.builder()
                .recommendedOption("Amazon (₹64,999)")
                .summary("Amazon costs ₹700 more than Croma, but has faster delivery.")
                .reasons(List.of("Higher seller rating: 4.6★", "Next-day delivery"))
                .tradeoffs(List.of("Paying ₹700 extra for faster delivery"))
                .confidence("HIGH")
                .deterministic(true)
                .contextType("PRODUCT")
                .build();

        when(decisionAdvisorService.advise(any())).thenReturn(mockRecommendation);

        String jsonPayload = """
                {
                    "contextType": "PRODUCT",
                    "userPriority": "BALANCED",
                    "productOffers": [
                        { "merchant": "Amazon", "price": 64999.00, "rating": 4.6 },
                        { "merchant": "Croma", "price": 64299.00, "rating": 4.1 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/advisor/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedOption").value("Amazon (₹64,999)"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.reasons[0]").value("Higher seller rating: 4.6★"))
                .andExpect(jsonPath("$.tradeoffs[0]").value("Paying ₹700 extra for faster delivery"));
    }

    @Test
    @DisplayName("GET /api/advisor/sample/product should return product sample")
    void testGetSampleProduct() throws Exception {
        DecisionRecommendation sample = DecisionRecommendation.builder()
                .recommendedOption("Amazon (₹64,999)")
                .summary("Sample product advice")
                .reasons(List.of("Top rated"))
                .tradeoffs(List.of("Premium price"))
                .confidence("HIGH")
                .contextType("PRODUCT")
                .build();

        when(decisionAdvisorService.getSampleProductAdvise()).thenReturn(sample);

        mockMvc.perform(get("/api/advisor/sample/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextType").value("PRODUCT"));
    }

    @Test
    @DisplayName("GET /api/advisor/sample/travel should return travel sample")
    void testGetSampleTravel() throws Exception {
        DecisionRecommendation sample = DecisionRecommendation.builder()
                .recommendedOption("IndiGo 6E-204 (₹5,450)")
                .summary("Sample travel advice")
                .reasons(List.of("Non-stop direct"))
                .tradeoffs(List.of("Costs ₹600 more"))
                .confidence("HIGH")
                .contextType("FLIGHT")
                .build();

        when(decisionAdvisorService.getSampleTravelAdvise()).thenReturn(sample);

        mockMvc.perform(get("/api/advisor/sample/travel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextType").value("FLIGHT"));
    }
}
