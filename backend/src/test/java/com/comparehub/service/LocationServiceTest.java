package com.comparehub.service;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.provider.impl.MapboxLocationProvider;
import com.comparehub.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocationServiceTest {

    private final LocationService locationService = new LocationServiceImpl(new MapboxLocationProvider());

    @Test
    void shouldReturnPlaceSuggestions() {
        List<PlaceSuggestionDto> suggestions = locationService.suggestPlaces("Connaught");

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.get(0).getMainText().contains("Connaught Place"));
    }

    @Test
    void shouldGeocodeKnownAddress() {
        LocationDto location = locationService.geocodeAddress("Connaught Place");

        assertNotNull(location);
        assertEquals(28.6315, location.getLatitude(), 0.01);
        assertEquals(77.2167, location.getLongitude(), 0.01);
    }

    @Test
    void shouldCalculateRouteEstimate() {
        LocationDto pickup = LocationDto.builder().latitude(28.6315).longitude(77.2167).address("Connaught Place").build();
        LocationDto drop = LocationDto.builder().latitude(28.5562).longitude(77.1000).address("IGI Airport").build();

        RouteEstimateResponseDto estimate = locationService.calculateRoute(pickup, drop);

        assertNotNull(estimate);
        assertTrue(estimate.getDistanceKm() > 5.0);
        assertTrue(estimate.getDurationMinutes() > 10);
        assertFalse(estimate.getPolylineCoordinates().isEmpty());
    }
}
