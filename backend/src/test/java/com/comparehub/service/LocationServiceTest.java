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

    @Test
    void shouldSuggestVitBhopalOnPartialInput() {
        List<PlaceSuggestionDto> suggestions = locationService.suggestPlaces("VIT B");
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.getMainText().toLowerCase().contains("vit bhopal")));

        PlaceSuggestionDto vit = suggestions.stream()
                .filter(s -> s.getMainText().toLowerCase().contains("vit bhopal"))
                .findFirst().orElseThrow();
        assertEquals(23.0775, vit.getLatitude(), 0.05);
        assertEquals(76.8513, vit.getLongitude(), 0.05);
        assertNotNull(vit.getFormattedAddress());
        assertNotNull(vit.getCity());
    }

    @Test
    void shouldSuggestIndoreAirportOnPartialInput() {
        List<PlaceSuggestionDto> suggestions = locationService.suggestPlaces("Indore Air");
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.getMainText().toLowerCase().contains("indore airport")));

        PlaceSuggestionDto ind = suggestions.stream()
                .filter(s -> s.getMainText().toLowerCase().contains("indore airport"))
                .findFirst().orElseThrow();
        assertEquals(22.7217, ind.getLatitude(), 0.05);
        assertEquals(75.8011, ind.getLongitude(), 0.05);
        assertEquals("Indore", ind.getCity());
    }

    @Test
    void shouldCalculateRouteForVitBhopalToSehore() {
        LocationDto pickup = locationService.geocodeAddress("VIT Bhopal University");
        LocationDto drop = locationService.geocodeAddress("Sehore");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = locationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertTrue(route.getDistanceKm() > 10.0, "Distance from VIT Bhopal to Sehore should be > 10km");
        assertTrue(route.getDurationMinutes() > 15);
        assertFalse(route.getPolylineCoordinates().isEmpty());
    }

    @Test
    void shouldCalculateRouteForIndoreAirportToRajwada() {
        LocationDto pickup = locationService.geocodeAddress("Indore Airport");
        LocationDto drop = locationService.geocodeAddress("Rajwada Palace");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = locationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertTrue(route.getDistanceKm() > 4.0, "Distance from Indore Airport to Rajwada Palace should be > 4km");
        assertTrue(route.getDurationMinutes() > 8);
    }

    @Test
    void shouldCalculateRouteForBhopalStationToAirport() {
        LocationDto pickup = locationService.geocodeAddress("Bhopal Railway Station");
        LocationDto drop = locationService.geocodeAddress("Bhopal Airport");

        RouteEstimateResponseDto route = locationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertTrue(route.getDistanceKm() > 8.0);
        assertTrue(route.getDurationMinutes() > 12);
    }

    @Test
    void shouldCalculateRouteForNewDelhiStationToIndiaGate() {
        LocationDto pickup = locationService.geocodeAddress("New Delhi Railway Station");
        LocationDto drop = locationService.geocodeAddress("India Gate");

        RouteEstimateResponseDto route = locationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertTrue(route.getDistanceKm() > 2.0);
        assertTrue(route.getDurationMinutes() > 5);
    }

    @Test
    void shouldCalculateRouteForMumbaiAirportToGatewayOfIndia() {
        LocationDto pickup = locationService.geocodeAddress("Mumbai Airport");
        LocationDto drop = locationService.geocodeAddress("Gateway of India");

        RouteEstimateResponseDto route = locationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertTrue(route.getDistanceKm() > 15.0);
        assertTrue(route.getDurationMinutes() > 25);
    }

    @Test
    void shouldReturnStructuredLocationDtoWithAllRequiredFields() {
        LocationDto loc = locationService.geocodeAddress("VIT Bhopal University");
        assertNotNull(loc.getName());
        assertNotNull(loc.getFormattedAddress());
        assertNotNull(loc.getLatitude());
        assertNotNull(loc.getLongitude());
        assertNotNull(loc.getCity());
        assertNotNull(loc.getState());
        assertNotNull(loc.getCountry());
        assertNotNull(loc.getProviderPlaceId());
    }

    @Test
    void shouldDynamicallyGeocodeArbitraryLocations() {
        List<PlaceSuggestionDto> suggestions = locationService.suggestPlaces("Anna Nagar Chennai");
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());

        PlaceSuggestionDto s = suggestions.get(0);
        assertNotNull(s.getLatitude());
        assertNotNull(s.getLongitude());
        assertNotNull(s.getFormattedAddress());
    }
}
