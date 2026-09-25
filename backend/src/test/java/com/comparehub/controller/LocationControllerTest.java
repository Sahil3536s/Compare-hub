package com.comparehub.controller;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.LocationService;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void shouldReturn404WhenGeocodeAddressNotFound() throws Exception {
        when(locationService.geocodeAddress("unknown-nonexistent-place")).thenReturn(null);

        mockMvc.perform(get("/api/location/geocode")
                        .param("address", "unknown-nonexistent-place")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No matching location found."));
    }

    @Test
    void shouldFailWhenAddressParamMissingInGeocode() throws Exception {
        mockMvc.perform(get("/api/location/geocode")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReverseGeocodeCoordinates() throws Exception {
        LocationDto location = LocationDto.builder()
                .latitude(23.2599)
                .longitude(77.4126)
                .name("Bhopal Junction")
                .address("Hamidia Road, Bhopal")
                .build();

        when(locationService.reverseGeocode(23.2599, 77.4126)).thenReturn(location);

        mockMvc.perform(get("/api/location/reverse-geocode")
                        .param("lat", "23.2599")
                        .param("lon", "77.4126")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bhopal Junction"))
                .andExpect(jsonPath("$.latitude").value(23.2599));
    }

    @Test
    void shouldFailWhenPickupMissingInRouteEstimate() throws Exception {
        LocationDto[] endpoints = new LocationDto[]{
                null,
                LocationDto.builder().latitude(28.5562).longitude(77.1000).address("IGI Airport").build()
        };

        mockMvc.perform(post("/api/location/route-estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpoints)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pickup location is required."));
    }

    @Test
    void shouldFailWhenDestinationMissingInRouteEstimate() throws Exception {
        LocationDto[] endpoints = new LocationDto[]{
                LocationDto.builder().latitude(28.6315).longitude(77.2167).address("Connaught Place").build(),
                null
        };

        mockMvc.perform(post("/api/location/route-estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpoints)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination location is required."));
    }

    @Test
    void shouldEstimateRouteSuccessfully() throws Exception {
        LocationDto pickup = LocationDto.builder().latitude(23.0775).longitude(76.8513).name("VIT Bhopal").build();
        LocationDto drop = LocationDto.builder().latitude(23.2875).longitude(77.3378).name("Bhopal Airport").build();

        RouteEstimateResponseDto estimate = RouteEstimateResponseDto.builder()
                .distanceKm(65.2)
                .durationMinutes(75)
                .routeSource("MAPBOX")
                .pickupAddress("VIT Bhopal")
                .dropAddress("Bhopal Airport")
                .polylineCoordinates(List.of(List.of(23.0775, 76.8513), List.of(23.2875, 77.3378)))
                .build();

        when(locationService.calculateRoute(any(), any())).thenReturn(estimate);

        mockMvc.perform(post("/api/location/route-estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocationDto[]{pickup, drop})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(65.2))
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.routeSource").value("MAPBOX"));
    }
}

