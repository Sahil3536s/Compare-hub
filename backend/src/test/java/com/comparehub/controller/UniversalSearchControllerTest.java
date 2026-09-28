package com.comparehub.controller;

import com.comparehub.dto.SearchIntentResultDto;
import com.comparehub.dto.UniversalSearchResponseDto;
import com.comparehub.model.SearchIntent;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.UniversalSearchService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UniversalSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class UniversalSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UniversalSearchService universalSearchService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/search should return 200 and UniversalSearchResponseDto")
    void testUniversalSearchEndpoint() throws Exception {
        SearchIntentResultDto intentDetails = SearchIntentResultDto.builder()
                .intent(SearchIntent.PRODUCT_SEARCH)
                .originalQuery("Samsung phone under 30000")
                .query("Samsung phone")
                .confidence(0.95)
                .filters(Map.of("brand", "Samsung"))
                .build();

        UniversalSearchResponseDto mockResponse = UniversalSearchResponseDto.builder()
                .intent(SearchIntent.PRODUCT_SEARCH)
                .query("Samsung phone under 30000")
                .redirectRoute("/shopping")
                .intentDetails(intentDetails)
                .executionTimeMs(45)
                .build();

        when(universalSearchService.executeUniversalSearch(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Samsung phone under 30000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("PRODUCT_SEARCH"))
                .andExpect(jsonPath("$.redirectRoute").value("/shopping"))
                .andExpect(jsonPath("$.intentDetails.query").value("Samsung phone"));
    }
}
