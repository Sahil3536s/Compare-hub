package com.comparehub.controller;

import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.FlightComparisonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlightComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class FlightComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlightComparisonService flightComparisonService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldSearchFlightsSuccessfully() throws Exception {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("DEL")
                .destination("BOM")
                .departureDate("2026-10-15")
                .adults(1)
                .cabinClass("ECONOMY")
                .build();

        NormalizedFlightOfferDto flight = NormalizedFlightOfferDto.builder()
                .provider("IndiGo Direct")
                .airline("IndiGo")
                .flightNumber("6E-5012")
                .origin("DEL")
                .destination("BOM")
                .departure("06:15")
                .arrival("08:25")
                .durationMinutes(130)
                .stops(0)
                .price(new BigDecimal("4999.00"))
                .currency("INR")
                .isCheapest(true)
                .isBest(true)
                .build();

        FlightComparisonResponseDto response = FlightComparisonResponseDto.builder()
                .origin("DEL")
                .destination("BOM")
                .departureDate("2026-10-15")
                .totalOffers(1)
                .cheapestPrice(new BigDecimal("4999.00"))
                .fastestDurationMinutes(130)
                .bestAirline("IndiGo")
                .offers(List.of(flight))
                .build();

        when(flightComparisonService.compareFlights(any(FlightSearchRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/flights/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("DEL"))
                .andExpect(jsonPath("$.destination").value("BOM"))
                .andExpect(jsonPath("$.totalOffers").value(1))
                .andExpect(jsonPath("$.offers[0].airline").value("IndiGo"))
                .andExpect(jsonPath("$.offers[0].isCheapest").value(true));
    }

    @Test
    void shouldFailValidationOnMissingOrigin() throws Exception {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("")
                .destination("BOM")
                .departureDate("2026-10-15")
                .build();

        mockMvc.perform(post("/api/flights/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
