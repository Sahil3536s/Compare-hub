package com.comparehub.controller;

import com.comparehub.dto.BudgetConstraint;
import com.comparehub.dto.BudgetOptimizationResponseDto;
import com.comparehub.dto.BudgetPlanOptionDto;
import com.comparehub.model.BudgetPreference;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.BudgetOptimizationService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetAssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetOptimizationService budgetOptimizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/budget/optimize should return optimized budget plans")
    void testOptimizeBudgetPlanEndpoint() throws Exception {
        BudgetPlanOptionDto plan = BudgetPlanOptionDto.builder()
                .id("plan-1")
                .title("IndiGo Express")
                .totalCost(BigDecimal.valueOf(17200))
                .remainingBudget(BigDecimal.valueOf(7800))
                .budgetUtilizationPercent(68.8)
                .isWithinBudget(true)
                .build();

        BudgetOptimizationResponseDto response = BudgetOptimizationResponseDto.builder()
                .maxBudget(BigDecimal.valueOf(25000))
                .plans(List.of(plan))
                .bestPlan(plan)
                .summaryText("Budget: ₹25,000. Found 1 option.")
                .build();

        when(budgetOptimizationService.optimizeBudgetPlan(any())).thenReturn(response);

        BudgetConstraint constraint = BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(25000))
                .build();

        mockMvc.perform(post("/api/budget/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(constraint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxBudget").value(25000))
                .andExpect(jsonPath("$.plans[0].totalCost").value(17200))
                .andExpect(jsonPath("$.plans[0].remainingBudget").value(7800));
    }

    @Test
    @DisplayName("POST /api/budget/parse-query should parse natural language string")
    void testParseQueryEndpoint() throws Exception {
        BudgetConstraint constraint = BudgetConstraint.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(3)
                .maxBudget(BigDecimal.valueOf(25000))
                .preference(BudgetPreference.BALANCED)
                .build();

        when(budgetOptimizationService.parseNaturalLanguageQuery(any())).thenReturn(constraint);

        mockMvc.perform(post("/api/budget/parse-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "3 people Delhi to Goa budget 25000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelers").value(3))
                .andExpect(jsonPath("$.origin").value("Delhi"))
                .andExpect(jsonPath("$.destination").value("Goa"))
                .andExpect(jsonPath("$.maxBudget").value(25000));
    }

    @Test
    @DisplayName("GET /api/budget/sample should return sample budget plan")
    void testGetSampleBudgetPlanEndpoint() throws Exception {
        BudgetOptimizationResponseDto response = BudgetOptimizationResponseDto.builder()
                .maxBudget(BigDecimal.valueOf(25000))
                .summaryText("Sample budget plan")
                .build();

        when(budgetOptimizationService.getSampleBudgetPlan()).thenReturn(response);

        mockMvc.perform(get("/api/budget/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxBudget").value(25000))
                .andExpect(jsonPath("$.summaryText").value("Sample budget plan"));
    }
}
