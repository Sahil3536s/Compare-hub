package com.comparehub.controller;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnPlaceSuggestions() throws Exception {
        PlaceSuggestionDto suggestion = PlaceSuggestionDto.builder()
                .placeId("del-cp")
                .mainText("Connaught Place")
                .fullAddress("Connaught Place, New Delhi")
                .latitude(28.6315)
                .longitude(77.2167)
                .build();

        when(locationService.suggestPlaces("Connaught")).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/location/suggest")
                        .param("q", "Connaught")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mainText").value("Connaught Place"))
                .andExpect(jsonPath("$[0].latitude").value(28.6315));
    }

    @Test
    void shouldGeocodeAddress() throws Exception {
        LocationDto location = LocationDto.builder()
                .latitude(28.6315)
                .longitude(77.2167)
                .address("Connaught Place, New Delhi")
                .build();

        when(locationService.geocodeAddress("Connaught Place")).thenReturn(location);

        mockMvc.perform(get("/api/location/geocode")
                        .param("address", "Connaught Place")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6315))
                .andExpect(jsonPath("$.address").value("Connaught Place, New Delhi"));
    }
}
