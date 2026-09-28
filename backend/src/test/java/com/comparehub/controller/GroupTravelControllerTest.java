package com.comparehub.controller;

import com.comparehub.dto.GroupTransportMode;
import com.comparehub.dto.GroupTravelOptionDto;
import com.comparehub.dto.GroupTravelRequestDto;
import com.comparehub.dto.GroupTravelResponseDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.GroupTravelService;
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

@WebMvcTest(GroupTravelController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupTravelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupTravelService groupTravelService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/travel/group-optimize should return 200 with optimized options")
    void testOptimizeGroupTravelEndpoint() throws Exception {
        GroupTravelOptionDto opt = GroupTravelOptionDto.builder()
                .id("opt_cab")
                .mode(GroupTransportMode.DIRECT_RIDE)
                .title("Direct Cab (Uber XL)")
                .totalCost(BigDecimal.valueOf(6400))
                .costPerPerson(BigDecimal.valueOf(1600))
                .numberOfTravelers(4)
                .build();

        GroupTravelResponseDto responseDto = GroupTravelResponseDto.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .numberOfTravelers(4)
                .options(List.of(opt))
                .bestValueOption(opt)
                .groupInsights("Group savings found")
                .build();

        when(groupTravelService.optimizeGroupTravel(any(GroupTravelRequestDto.class))).thenReturn(responseDto);

        GroupTravelRequestDto request = GroupTravelRequestDto.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .numberOfTravelers(4)
                .priority("CHEAPEST")
                .build();

        mockMvc.perform(post("/api/travel/group-optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfTravelers").value(4))
                .andExpect(jsonPath("$.options[0].id").value("opt_cab"));
    }

    @Test
    @DisplayName("GET /api/travel/group-optimize should return 200 with optimized options")
    void testGetGroupTravelOptimizationEndpoint() throws Exception {
        GroupTravelResponseDto responseDto = GroupTravelResponseDto.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .numberOfTravelers(4)
                .options(List.of())
                .build();

        when(groupTravelService.optimizeGroupTravel(any(GroupTravelRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(get("/api/travel/group-optimize")
                        .param("origin", "Delhi")
                        .param("destination", "Jaipur")
                        .param("numberOfTravelers", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfTravelers").value(4));
    }
}
