package com.comparehub.controller;

import com.comparehub.dto.JourneyOptionDto;
import com.comparehub.dto.SmartJourneyRequestDto;
import com.comparehub.dto.SmartJourneyResponseDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.JourneyOptimizationService;
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

@WebMvcTest(SmartJourneyController.class)
@AutoConfigureMockMvc(addFilters = false)
class SmartJourneyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JourneyOptimizationService journeyOptimizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/journey/optimize should return optimized door-to-door combinations")
    void testOptimizeJourneyEndpoint() throws Exception {
        JourneyOptionDto balanced = JourneyOptionDto.builder()
                .id("opt-1")
                .title("IndiGo Express + Uber")
                .totalCost(BigDecimal.valueOf(5820))
                .totalDurationMinutes(320)
                .formattedTotalDuration("5h 20m")
                .transferCount(2)
                .travelerCount(1)
                .build();

        SmartJourneyResponseDto response = SmartJourneyResponseDto.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(1)
                .balancedJourney(balanced)
                .topRecommendedJourney(balanced)
                .allCombinations(List.of(balanced))
                .tradeoffSummary("Optimal balance between price and transit time.")
                .build();

        when(journeyOptimizationService.optimizeJourney(any())).thenReturn(response);

        SmartJourneyRequestDto request = SmartJourneyRequestDto.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(1)
                .build();

        mockMvc.perform(post("/api/journey/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("Delhi"))
                .andExpect(jsonPath("$.destination").value("Goa"))
                .andExpect(jsonPath("$.topRecommendedJourney.title").value("IndiGo Express + Uber"))
                .andExpect(jsonPath("$.topRecommendedJourney.totalCost").value(5820));
    }

    @Test
    @DisplayName("GET /api/journey/sample should return sample door-to-door itinerary")
    void testGetSampleJourneyEndpoint() throws Exception {
        JourneyOptionDto sample = JourneyOptionDto.builder()
                .id("sample-1")
                .title("Sample Journey")
                .totalCost(BigDecimal.valueOf(6170))
                .totalDurationMinutes(320)
                .build();

        SmartJourneyResponseDto response = SmartJourneyResponseDto.builder()
                .origin("Delhi")
                .destination("Goa")
                .topRecommendedJourney(sample)
                .build();

        when(journeyOptimizationService.getSampleJourney(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(response);

        mockMvc.perform(get("/api/journey/sample?origin=Delhi&destination=Goa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("Delhi"))
                .andExpect(jsonPath("$.topRecommendedJourney.totalCost").value(6170));
    }
}
