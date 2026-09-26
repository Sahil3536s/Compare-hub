package com.comparehub.service;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.provider.impl.MapboxLocationProvider;
import com.comparehub.service.impl.LocationServiceImpl;
import com.comparehub.exception.ProviderUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LocationServiceTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;
    private ObjectMapper objectMapper;

    private MapboxLocationProvider mockProvider;
    private LocationService mockLocationService;

    private MapboxLocationProvider fallbackProvider;
    private LocationService fallbackLocationService;

    private MapboxLocationProvider strictNoTokenProvider;
    private LocationService strictNoTokenLocationService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        objectMapper = new ObjectMapper();

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);

        mockProvider = new MapboxLocationProvider(restClient, objectMapper, "pk.test-token-12345", false, "in");
        mockLocationService = new LocationServiceImpl(mockProvider);

        fallbackProvider = new MapboxLocationProvider(null, objectMapper, null, true, "in");
        fallbackLocationService = new LocationServiceImpl(fallbackProvider);

        strictNoTokenProvider = new MapboxLocationProvider(null, objectMapper, null, false, "in");
        strictNoTokenLocationService = new LocationServiceImpl(strictNoTokenProvider);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Five Core Real Routes (Geocoding & Route Calculation)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCalculateRouteForVitBhopalToBhopalAirport() {
        String vitJson = """
                {"features": [{"id": "place.vit", "text": "VIT Bhopal University", "place_name": "VIT Bhopal University, Sehore, Madhya Pradesh, India", "center": [76.8513, 23.0775], "context": [{"id": "place.sehore", "text": "Sehore"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String bhoJson = """
                {"features": [{"id": "place.bho", "text": "Bhopal Airport", "place_name": "Raja Bhoj Airport, Bhopal, Madhya Pradesh, India", "center": [77.3378, 23.2875], "context": [{"id": "place.bhopal", "text": "Bhopal"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String directionsJson = """
                {"routes": [{"distance": 68400.0, "duration": 4800.0, "geometry": {"coordinates": [[76.8513, 23.0775], [77.1000, 23.1800], [77.3378, 23.2875]]}}]}
                """;

        when(responseSpec.body(String.class)).thenReturn(vitJson, bhoJson, directionsJson);

        LocationDto pickup = mockLocationService.geocodeAddress("VIT Bhopal University");
        LocationDto drop = mockLocationService.geocodeAddress("Bhopal Airport");

        assertNotNull(pickup);
        assertEquals(23.0775, pickup.getLatitude(), 0.001);
        assertEquals(76.8513, pickup.getLongitude(), 0.001);

        assertNotNull(drop);
        assertEquals(23.2875, drop.getLatitude(), 0.001);
        assertEquals(77.3378, drop.getLongitude(), 0.001);

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertEquals(68.4, route.getDistanceKm(), 0.1);
        assertEquals(80, route.getDurationMinutes());
        assertEquals("MAPBOX", route.getRouteSource());
        assertFalse(route.getPolylineCoordinates().isEmpty());
    }

    @Test
    void shouldCalculateRouteForBhopalJunctionToDbMall() {
        String stnJson = """
                {"features": [{"id": "place.stn", "text": "Bhopal Junction", "place_name": "Bhopal Railway Station, Bhopal, Madhya Pradesh, India", "center": [77.4126, 23.2599], "context": [{"id": "place.bho", "text": "Bhopal"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String mallJson = """
                {"features": [{"id": "place.mall", "text": "DB City Mall", "place_name": "DB City Mall, Arera Hills, Bhopal, Madhya Pradesh, India", "center": [77.4332, 23.2330], "context": [{"id": "place.bho", "text": "Bhopal"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String directionsJson = """
                {"routes": [{"distance": 5800.0, "duration": 840.0, "geometry": {"coordinates": [[77.4126, 23.2599], [77.4332, 23.2330]]}}]}
                """;

        when(responseSpec.body(String.class)).thenReturn(stnJson, mallJson, directionsJson);

        LocationDto pickup = mockLocationService.geocodeAddress("Bhopal Junction");
        LocationDto drop = mockLocationService.geocodeAddress("DB City Mall");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertEquals(5.8, route.getDistanceKm(), 0.1);
        assertEquals(14, route.getDurationMinutes());
        assertEquals("MAPBOX", route.getRouteSource());
    }

    @Test
    void shouldCalculateRouteForIndoreAirportToRajwadaPalace() {
        String indAirJson = """
                {"features": [{"id": "place.indair", "text": "Indore Airport", "place_name": "Devi Ahilyabai Holkar Airport, Indore, Madhya Pradesh, India", "center": [75.8011, 22.7217], "context": [{"id": "place.ind", "text": "Indore"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String rajwadaJson = """
                {"features": [{"id": "place.rajwada", "text": "Rajwada Palace", "place_name": "Rajwada Palace, MG Road, Indore, Madhya Pradesh, India", "center": [75.8553, 22.7186], "context": [{"id": "place.ind", "text": "Indore"}, {"id": "region.mp", "text": "Madhya Pradesh"}]}]}
                """;
        String directionsJson = """
                {"routes": [{"distance": 8100.0, "duration": 1200.0, "geometry": {"coordinates": [[75.8011, 22.7217], [75.8553, 22.7186]]}}]}
                """;

        when(responseSpec.body(String.class)).thenReturn(indAirJson, rajwadaJson, directionsJson);

        LocationDto pickup = mockLocationService.geocodeAddress("Indore Airport");
        LocationDto drop = mockLocationService.geocodeAddress("Rajwada Palace");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertEquals(8.1, route.getDistanceKm(), 0.1);
        assertEquals(20, route.getDurationMinutes());
        assertEquals("MAPBOX", route.getRouteSource());
    }

    @Test
    void shouldCalculateRouteForNewDelhiStationToIndiaGate() {
        String ndlsJson = """
                {"features": [{"id": "place.ndls", "text": "New Delhi Railway Station", "place_name": "New Delhi Railway Station, New Delhi, Delhi, India", "center": [77.2195, 28.6429], "context": [{"id": "place.del", "text": "New Delhi"}, {"id": "region.del", "text": "Delhi"}]}]}
                """;
        String indiaGateJson = """
                {"features": [{"id": "place.ig", "text": "India Gate", "place_name": "India Gate, Kartavya Path, New Delhi, Delhi, India", "center": [77.2295, 28.6129], "context": [{"id": "place.del", "text": "New Delhi"}, {"id": "region.del", "text": "Delhi"}]}]}
                """;
        String directionsJson = """
                {"routes": [{"distance": 4500.0, "duration": 720.0, "geometry": {"coordinates": [[77.2195, 28.6429], [77.2295, 28.6129]]}}]}
                """;

        when(responseSpec.body(String.class)).thenReturn(ndlsJson, indiaGateJson, directionsJson);

        LocationDto pickup = mockLocationService.geocodeAddress("New Delhi Railway Station");
        LocationDto drop = mockLocationService.geocodeAddress("India Gate");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertEquals(4.5, route.getDistanceKm(), 0.1);
        assertEquals(12, route.getDurationMinutes());
        assertEquals("MAPBOX", route.getRouteSource());
    }

    @Test
    void shouldCalculateRouteForMumbaiAirportToGatewayOfIndia() {
        String bomAirJson = """
                {"features": [{"id": "place.bom", "text": "Mumbai Airport", "place_name": "CSMIA, Mumbai, Maharashtra, India", "center": [72.8656, 19.0896], "context": [{"id": "place.bomc", "text": "Mumbai"}, {"id": "region.mh", "text": "Maharashtra"}]}]}
                """;
        String gatewayJson = """
                {"features": [{"id": "place.gw", "text": "Gateway of India", "place_name": "Gateway of India, Colaba, Mumbai, Maharashtra, India", "center": [72.8347, 18.9220], "context": [{"id": "place.bomc", "text": "Mumbai"}, {"id": "region.mh", "text": "Maharashtra"}]}]}
                """;
        String directionsJson = """
                {"routes": [{"distance": 24200.0, "duration": 2880.0, "geometry": {"coordinates": [[72.8656, 19.0896], [72.8347, 18.9220]]}}]}
                """;

        when(responseSpec.body(String.class)).thenReturn(bomAirJson, gatewayJson, directionsJson);

        LocationDto pickup = mockLocationService.geocodeAddress("Mumbai Airport");
        LocationDto drop = mockLocationService.geocodeAddress("Gateway of India");

        assertNotNull(pickup);
        assertNotNull(drop);

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);
        assertNotNull(route);
        assertEquals(24.2, route.getDistanceKm(), 0.1);
        assertEquals(48, route.getDurationMinutes());
        assertEquals("MAPBOX", route.getRouteSource());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Elimination of Fake Coordinates for Unknown / Nonsense Locations
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyWhenSearchingUnknownOrNonsenseLocation() {
        String emptyJson = "{\"features\": []}";
        when(responseSpec.body(String.class)).thenReturn(emptyJson);

        List<PlaceSuggestionDto> suggestions = mockLocationService.suggestPlaces("zzzzxxxyyyy123");

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty(), "Unknown locations must return empty list, NOT fake coordinates!");
    }

    @Test
    void shouldReturnNullWhenGeocodingUnknownOrNonsenseLocation() {
        String emptyJson = "{\"features\": []}";
        when(responseSpec.body(String.class)).thenReturn(emptyJson);

        LocationDto location = mockLocationService.geocodeAddress("zzzzxxxyyyy123");

        assertNull(location, "Geocoding unknown locations must return null, NOT fake coordinates!");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Rejection of Missing Coordinates in calculateRoute
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldRejectRouteCalculationWhenPickupCoordinatesAreMissing() {
        LocationDto pickup = LocationDto.builder().name("Missing Coords Pickup").build();
        LocationDto drop = LocationDto.builder().latitude(28.5562).longitude(77.1000).name("Valid Drop").build();

        assertThrows(IllegalArgumentException.class, () -> mockLocationService.calculateRoute(pickup, drop));
    }

    @Test
    void shouldRejectRouteCalculationWhenDestinationCoordinatesAreMissing() {
        LocationDto pickup = LocationDto.builder().latitude(28.6315).longitude(77.2167).name("Valid Pickup").build();
        LocationDto drop = LocationDto.builder().name("Missing Coords Drop").build();

        assertThrows(IllegalArgumentException.class, () -> mockLocationService.calculateRoute(pickup, drop));
    }

    @Test
    void shouldRejectRouteCalculationWhenEndpointsAreNull() {
        LocationDto valid = LocationDto.builder().latitude(28.6315).longitude(77.2167).build();
        assertThrows(IllegalArgumentException.class, () -> mockLocationService.calculateRoute(null, valid));
        assertThrows(IllegalArgumentException.class, () -> mockLocationService.calculateRoute(valid, null));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Route Source & Fallback to Mathematical Routing on Real Coordinates
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldMarkRouteSourceAsEstimatedWhenMapboxDirectionsFails() {
        when(responseSpec.body(String.class)).thenThrow(new RestClientException("Directions 500 error"));

        LocationDto pickup = LocationDto.builder().latitude(28.6315).longitude(77.2167).name("Connaught Place").build();
        LocationDto drop = LocationDto.builder().latitude(28.5562).longitude(77.1000).name("IGI Airport").build();

        RouteEstimateResponseDto route = mockLocationService.calculateRoute(pickup, drop);

        assertNotNull(route);
        assertEquals("ESTIMATED", route.getRouteSource());
        assertTrue(route.getDistanceKm() > 10.0);
        assertTrue(route.getDurationMinutes() > 10);
        assertFalse(route.getPolylineCoordinates().isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Reverse Geocoding
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReverseGeocodeSuccessfullyWithRealMapboxResponse() {
        String revgeoJson = """
                {"features": [{"id": "place.rev", "text": "Connaught Place", "place_name": "Connaught Place, New Delhi, Delhi, India", "center": [77.2167, 28.6315], "context": [{"id": "place.del", "text": "New Delhi"}, {"id": "region.del", "text": "Delhi"}]}]}
                """;
        when(responseSpec.body(String.class)).thenReturn(revgeoJson);

        LocationDto loc = mockLocationService.reverseGeocode(28.6315, 77.2167);

        assertNotNull(loc);
        assertEquals("Connaught Place", loc.getName());
        assertEquals("New Delhi", loc.getCity());
        assertEquals("Delhi", loc.getState());
        assertEquals(28.6315, loc.getLatitude());
        assertEquals(77.2167, loc.getLongitude());
    }

    @Test
    void shouldReturnCoordinateBasedLocationWhenReverseGeocodingFailsWithoutInventingStreet() {
        when(responseSpec.body(String.class)).thenThrow(new RestClientException("Mapbox down"));

        LocationDto loc = mockLocationService.reverseGeocode(25.12345, 78.54321);

        assertNotNull(loc);
        assertEquals("Current Coordinates", loc.getName());
        assertEquals("Location (25.12345, 78.54321)", loc.getFormattedAddress());
        assertEquals(25.12345, loc.getLatitude(), 0.0001);
        assertEquals(78.54321, loc.getLongitude(), 0.0001);
    }

    @Test
    void shouldThrowExceptionWhenReverseGeocodeCoordinatesAreNull() {
        assertThrows(IllegalArgumentException.class, () -> mockLocationService.reverseGeocode(null, 77.0));
        assertThrows(IllegalArgumentException.class, () -> mockLocationService.reverseGeocode(28.0, null));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Network Failure & Missing Token Resilience
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldHandleNetworkTimeoutGracefullyWithoutCrashing() {
        when(responseSpec.body(String.class)).thenThrow(new RestClientException("Connection timeout"));

        assertThrows(ProviderUnavailableException.class, () -> mockLocationService.suggestPlaces("VIT Bhopal"));
        assertThrows(ProviderUnavailableException.class, () -> mockLocationService.geocodeAddress("VIT Bhopal"));
    }

    @Test
    void shouldHandleMissingTokenWithoutCrashing() {
        assertThrows(ProviderUnavailableException.class, () -> strictNoTokenLocationService.suggestPlaces("VIT Bhopal"));
        assertThrows(ProviderUnavailableException.class, () -> strictNoTokenLocationService.geocodeAddress("VIT Bhopal"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Strict Fallback Mode Toggle
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldAllowKnownHubsWhenFallbackIsEnabled() {
        List<PlaceSuggestionDto> suggestions = fallbackLocationService.suggestPlaces("VIT B");
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.get(0).getMainText().contains("VIT Bhopal"));

        LocationDto geocode = fallbackLocationService.geocodeAddress("Connaught Place");
        assertNotNull(geocode);
        assertEquals(28.6315, geocode.getLatitude(), 0.01);
    }

    @Test
    void shouldDisallowKnownHubsWhenFallbackIsDisabled() {
        assertThrows(ProviderUnavailableException.class, () -> strictNoTokenLocationService.suggestPlaces("VIT B"));
        assertThrows(ProviderUnavailableException.class, () -> strictNoTokenLocationService.geocodeAddress("Connaught Place"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Real Dynamic Place Autocomplete & Parsing (IFFCO Chowk, Cyber City, etc.)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDynamicallySearchAndParseIffcoChowkWithRealCoordinates() {
        String iffcoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "id": "poi.515396160867",
                      "type": "Feature",
                      "place_type": ["poi"],
                      "relevance": 1,
                      "properties": {
                        "address": "Mehrauli-Gurgaon Road"
                      },
                      "text": "IFFCO Chowk",
                      "place_name": "IFFCO Chowk Metro Station, Mehrauli-Gurgaon Road, Gurugram, Haryana 122002, India",
                      "center": [77.0722, 28.4720],
                      "context": [
                        { "id": "neighborhood.123", "text": "Sector 29" },
                        { "id": "locality.456", "text": "Gurgaon" },
                        { "id": "place.789", "text": "Gurugram" },
                        { "id": "region.101", "text": "Haryana" },
                        { "id": "country.102", "text": "India" }
                      ]
                    }
                  ]
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(iffcoJson);

        List<PlaceSuggestionDto> results = mockLocationService.suggestPlaces("IFFCO Chowk");

        assertNotNull(results);
        assertEquals(1, results.size());

        PlaceSuggestionDto place = results.get(0);
        assertEquals("poi.515396160867", place.getPlaceId());
        assertEquals("IFFCO Chowk", place.getMainText());
        assertEquals("IFFCO Chowk", place.getName());
        assertEquals("IFFCO Chowk Metro Station, Mehrauli-Gurgaon Road, Gurugram, Haryana 122002, India", place.getFullAddress());
        assertEquals("IFFCO Chowk Metro Station, Mehrauli-Gurgaon Road, Gurugram, Haryana 122002, India", place.getFormattedAddress());
        assertEquals(28.4720, place.getLatitude(), 0.0001);
        assertEquals(77.0722, place.getLongitude(), 0.0001);
        assertEquals("Gurugram", place.getCity());
        assertEquals("Haryana", place.getState());
        assertEquals("India", place.getCountry());
        assertEquals("poi.515396160867", place.getProviderPlaceId());
        assertFalse(place.getSecondaryText().isEmpty());
    }

    @Test
    void shouldParsePlaceWithoutCityOrStateGracefully() {
        String minimalJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "id": "address.999",
                      "text": "Unnamed Road",
                      "place_name": "Unnamed Road, India",
                      "center": [77.1000, 28.5000]
                    }
                  ]
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(minimalJson);

        List<PlaceSuggestionDto> results = mockLocationService.suggestPlaces("Unnamed Road");

        assertNotNull(results);
        assertEquals(1, results.size());
        PlaceSuggestionDto item = results.get(0);
        assertEquals("Unnamed Road", item.getMainText());
        assertEquals(28.5000, item.getLatitude(), 0.001);
        assertEquals(77.1000, item.getLongitude(), 0.001);
        assertEquals("", item.getCity());
        assertEquals("", item.getState());
        assertEquals("India", item.getCountry());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 9. Configurable Country Filter
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldIncludeCountryParamWhenCountryFilterIsConfigured() {
        String emptyJson = "{\"features\": []}";
        when(responseSpec.body(String.class)).thenReturn(emptyJson);

        mockProvider.setCountryFilter("in");
        mockLocationService.suggestPlaces("Cyber City");

        verify(uriSpec).uri(argThat((String url) -> url != null && url.contains("&country=in")));
    }

    @Test
    void shouldOmitCountryParamWhenCountryFilterIsBlank() {
        String emptyJson = "{\"features\": []}";
        when(responseSpec.body(String.class)).thenReturn(emptyJson);

        mockProvider.setCountryFilter("");
        mockLocationService.suggestPlaces("Cyber City");

        verify(uriSpec).uri(argThat((String url) -> url != null && !url.contains("&country=")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 10. All 10 Target Indian Places Dynamic Search & Structure Verification
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDynamicallySearchAllTargetIndianPlacesWithRealCoordinates() {
        record TargetPlace(String query, String json, double expectedLat, double expectedLon, String expectedCity) {}

        List<TargetPlace> targets = List.of(
                new TargetPlace("IFFCO Chowk",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.1","text":"IFFCO Chowk","place_name":"IFFCO Chowk Metro Station, Gurugram, Haryana, India","center":[77.0722,28.4720],"context":[{"id":"place.1","text":"Gurugram"},{"id":"region.1","text":"Haryana"}]}]}
                        """, 28.4720, 77.0722, "Gurugram"),
                new TargetPlace("Cyber City Gurgaon",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.2","text":"Cyber City","place_name":"DLF Cyber City, Gurugram, Haryana, India","center":[77.0895,28.4950],"context":[{"id":"place.1","text":"Gurugram"},{"id":"region.1","text":"Haryana"}]}]}
                        """, 28.4950, 77.0895, "Gurugram"),
                new TargetPlace("VIT Bhopal University",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.3","text":"VIT Bhopal University","place_name":"VIT Bhopal University, Sehore, Madhya Pradesh, India","center":[76.8513,23.0775],"context":[{"id":"place.2","text":"Sehore"},{"id":"region.2","text":"Madhya Pradesh"}]}]}
                        """, 23.0775, 76.8513, "Sehore"),
                new TargetPlace("Bhopal Junction",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.4","text":"Bhopal Junction","place_name":"Bhopal Junction Railway Station, Bhopal, Madhya Pradesh, India","center":[77.4126,23.2599],"context":[{"id":"place.3","text":"Bhopal"},{"id":"region.2","text":"Madhya Pradesh"}]}]}
                        """, 23.2599, 77.4126, "Bhopal"),
                new TargetPlace("DB Mall Bhopal",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.5","text":"DB City Mall","place_name":"DB City Mall, Arera Hills, Bhopal, Madhya Pradesh, India","center":[77.4332,23.2330],"context":[{"id":"place.3","text":"Bhopal"},{"id":"region.2","text":"Madhya Pradesh"}]}]}
                        """, 23.2330, 77.4332, "Bhopal"),
                new TargetPlace("Rajwada Palace Indore",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.6","text":"Rajwada Palace","place_name":"Rajwada Palace, MG Road, Indore, Madhya Pradesh, India","center":[75.8553,22.7186],"context":[{"id":"place.4","text":"Indore"},{"id":"region.2","text":"Madhya Pradesh"}]}]}
                        """, 22.7186, 75.8553, "Indore"),
                new TargetPlace("India Gate",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.7","text":"India Gate","place_name":"India Gate, Rajpath, New Delhi, Delhi, India","center":[77.2295,28.6129],"context":[{"id":"place.5","text":"New Delhi"},{"id":"region.3","text":"Delhi"}]}]}
                        """, 28.6129, 77.2295, "New Delhi"),
                new TargetPlace("New Delhi Railway Station",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.8","text":"New Delhi Railway Station","place_name":"New Delhi Railway Station, Paharganj, New Delhi, Delhi, India","center":[77.2195,28.6429],"context":[{"id":"place.5","text":"New Delhi"},{"id":"region.3","text":"Delhi"}]}]}
                        """, 28.6429, 77.2195, "New Delhi"),
                new TargetPlace("Gateway of India",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.9","text":"Gateway of India","place_name":"Gateway of India, Colaba, Mumbai, Maharashtra, India","center":[72.8347,18.9220],"context":[{"id":"place.6","text":"Mumbai"},{"id":"region.4","text":"Maharashtra"}]}]}
                        """, 18.9220, 72.8347, "Mumbai"),
                new TargetPlace("Mumbai Airport",
                        """
                        {"type":"FeatureCollection","features":[{"id":"poi.10","text":"Mumbai Airport","place_name":"CSMIA, Sahar, Mumbai, Maharashtra, India","center":[72.8656,19.0896],"context":[{"id":"place.6","text":"Mumbai"},{"id":"region.4","text":"Maharashtra"}]}]}
                        """, 19.0896, 72.8656, "Mumbai")
        );

        for (TargetPlace target : targets) {
            when(responseSpec.body(String.class)).thenReturn(target.json());
            List<PlaceSuggestionDto> suggestions = mockLocationService.suggestPlaces(target.query());
            assertNotNull(suggestions, "Suggestions should not be null for: " + target.query());
            assertFalse(suggestions.isEmpty(), "Suggestions should not be empty for: " + target.query());

            PlaceSuggestionDto place = suggestions.get(0);
            assertNotNull(place.getPlaceId());
            assertNotNull(place.getMainText());
            assertNotNull(place.getName());
            assertNotNull(place.getFormattedAddress());
            assertNotNull(place.getFullAddress());
            assertEquals(target.expectedLat(), place.getLatitude(), 0.001, "Latitude mismatch for: " + target.query());
            assertEquals(target.expectedLon(), place.getLongitude(), 0.001, "Longitude mismatch for: " + target.query());
            assertEquals(target.expectedCity(), place.getCity(), "City mismatch for: " + target.query());
            assertEquals("India", place.getCountry());
        }
    }
}

