package com.comparehub.controller;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.AirportSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AirportSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class AirportSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AirportSearchService airportSearchService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnAirportsOnSearch() throws Exception {
        AirportResultDto del = AirportResultDto.builder()
                .name("Indira Gandhi International Airport")
                .iataCode("DEL")
                .cityName("Delhi")
                .countryName("India")
                .airportType("AIRPORT")
                .latitude(28.5562)
                .longitude(77.1000)
                .build();

        when(airportSearchService.searchAirports("Delhi")).thenReturn(List.of(del));

        mockMvc.perform(get("/api/airports/search")
                        .param("q", "Delhi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iataCode").value("DEL"))
                .andExpect(jsonPath("$[0].cityName").value("Delhi"))
                .andExpect(jsonPath("$[0].name").value("Indira Gandhi International Airport"))
                .andExpect(jsonPath("$[0].countryName").value("India"));
    }

    @Test
    void shouldReturnEmptyListForUnknownPlace() throws Exception {
        when(airportSearchService.searchAirports(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/airports/search")
                        .param("q", "NoSuchCity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
