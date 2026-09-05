package com.comparehub.controller;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.dto.RideCompareRequestDto;
import com.comparehub.dto.RideComparisonResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.RideComparisonService;
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

@WebMvcTest(RideComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class RideComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RideComparisonService rideComparisonService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldCompareRidesSuccessfully() throws Exception {
        LocationDto pickup = LocationDto.builder().latitude(28.6315).longitude(77.2167).address("Connaught Place").build();
        LocationDto drop = LocationDto.builder().latitude(28.5562).longitude(77.1000).address("IGI Airport").build();

        RideCompareRequestDto request = RideCompareRequestDto.builder()
                .pickup(pickup)
                .destination(drop)
                .rideType("all")
                .sortBy("best")
                .build();

        NormalizedRideOfferDto offer = NormalizedRideOfferDto.builder()
                .provider("Uber")
                .rideType("Uber Go")
                .vehicleCategory("Cab")
                .estimatedPriceMin(new BigDecimal("280.00"))
                .estimatedPriceMax(new BigDecimal("320.00"))
                .etaMinutes(3)
                .distanceKm(14.2)
                .isCheapest(true)
                .isBest(true)
                .build();

        RideComparisonResponseDto response = RideComparisonResponseDto.builder()
                .pickup(pickup)
                .destination(drop)
                .distanceKm(14.2)
                .durationMinutes(32)
                .cheapestFare(new BigDecimal("280.00"))
                .fastestEtaMinutes(3)
                .bestProvider("Uber (Uber Go)")
                .offers(List.of(offer))
                .build();

        when(rideComparisonService.compareRides(any(RideCompareRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(14.2))
                .andExpect(jsonPath("$.durationMinutes").value(32))
                .andExpect(jsonPath("$.offers[0].provider").value("Uber"))
                .andExpect(jsonPath("$.offers[0].isCheapest").value(true));
    }
}
