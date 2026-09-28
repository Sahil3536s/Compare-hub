package com.comparehub.controller;

import com.comparehub.dto.DecisionRecommendation;
import com.comparehub.dto.UnifiedDecisionRequest;
import com.comparehub.dto.UnifiedDecisionResponse;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.DecisionEngineService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionEngineController.class)
@AutoConfigureMockMvc(addFilters = false)
class DecisionEngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DecisionEngineService decisionEngineService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/decision/evaluate should return unified decision response")
    void testEvaluateDecisionEndpoint() throws Exception {
        UnifiedDecisionResponse mockResponse = UnifiedDecisionResponse.builder()
                .decisionType("PRODUCT")
                .query("iPhone 15")
                .estimatedSavings(BigDecimal.valueOf(1500.00))
                .decisionAdvisor(DecisionRecommendation.builder()
                        .recommendedOption("Amazon (₹64,999)")
                        .summary("Amazon offers best overall value.")
                        .confidence("HIGH")
                        .build())
                .pipelineAudit(List.of("NLU_INTENT_PARSING", "MULTI_MERCHANT_PROVIDER_SEARCH", "AI_DECISION_ADVISOR_2_0"))
                .build();

        when(decisionEngineService.evaluate(any())).thenReturn(mockResponse);

        UnifiedDecisionRequest request = UnifiedDecisionRequest.builder()
                .query("iPhone 15")
                .decisionType("PRODUCT")
                .build();

        mockMvc.perform(post("/api/decision/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionType").value("PRODUCT"))
                .andExpect(jsonPath("$.estimatedSavings").value(1500.00))
                .andExpect(jsonPath("$.decisionAdvisor.recommendedOption").value("Amazon (₹64,999)"))
                .andExpect(jsonPath("$.pipelineAudit[0]").value("NLU_INTENT_PARSING"));
    }

    @Test
    @DisplayName("GET /api/decision/sample/{type} should return sample decision")
    void testGetSampleByTypeEndpoint() throws Exception {
        UnifiedDecisionResponse mockSample = UnifiedDecisionResponse.builder()
                .decisionType("JOURNEY")
                .query("Door-to-Door Journey: Delhi to Goa (2 travelers)")
                .estimatedSavings(BigDecimal.valueOf(2400.00))
                .build();

        when(decisionEngineService.evaluateSample("JOURNEY")).thenReturn(mockSample);

        mockMvc.perform(get("/api/decision/sample/JOURNEY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionType").value("JOURNEY"))
                .andExpect(jsonPath("$.estimatedSavings").value(2400.00));
    }
}
