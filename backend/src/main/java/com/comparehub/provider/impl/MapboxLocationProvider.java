package com.comparehub.provider.impl;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.exception.ProviderUnavailableException;
import com.comparehub.provider.LocationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MapboxLocationProvider implements LocationProvider {

    @Value("${app.location.mapbox.access-token:${MAPBOX_ACCESS_TOKEN:}}")
    private String mapboxAccessToken;

    @Value("${app.location.fallback-enabled:${LOCATION_FALLBACK_MODE:false}}")
    private boolean fallbackEnabled = false;

    @Value("${app.location.mapbox.country-filter:${LOCATION_COUNTRY_FILTER:in}}")
    private String countryFilter = "in";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MapboxLocationProvider() {
        this(RestClient.create(), new ObjectMapper());
    }

    public MapboxLocationProvider(RestClient restClient, ObjectMapper objectMapper) {
        this(restClient, objectMapper, null, false, "in");
    }

    public MapboxLocationProvider(RestClient restClient, ObjectMapper objectMapper, String mapboxAccessToken, boolean fallbackEnabled, String countryFilter) {
        this.restClient = restClient != null ? restClient : RestClient.create();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.mapboxAccessToken = mapboxAccessToken;
        this.fallbackEnabled = fallbackEnabled;
        this.countryFilter = countryFilter != null ? countryFilter : "in";
        loadConfiguration();
    }

    @PostConstruct
    public void init() {
        loadConfiguration();
        log.info("Mapbox configured: {}", isMapboxTokenValid());
    }

    public void loadConfiguration() {
        if (mapboxAccessToken == null || mapboxAccessToken.trim().isEmpty()) {
            String envToken = System.getenv("MAPBOX_ACCESS_TOKEN");
            if (envToken != null && !envToken.isBlank()) {
                mapboxAccessToken = envToken.trim();
            } else {
                String propToken = System.getProperty("MAPBOX_ACCESS_TOKEN");
                if (propToken != null && !propToken.isBlank()) {
                    mapboxAccessToken = propToken.trim();
                } else {
                    mapboxAccessToken = tryLoadTokenFromDotEnv();
                }
            }
        }
        if (mapboxAccessToken != null) {
            mapboxAccessToken = mapboxAccessToken.trim().replaceAll("^[\"']|[\"']$", "");
        }
        if (countryFilter == null || countryFilter.trim().isEmpty()) {
            String envCountry = System.getenv("LOCATION_COUNTRY_FILTER");
            if (envCountry != null && !envCountry.isBlank()) {
                countryFilter = envCountry.trim();
            }
        }
        if (countryFilter != null) {
            countryFilter = countryFilter.trim().replaceAll("^[\"']|[\"']$", "");
        }
    }

    private String tryLoadTokenFromDotEnv() {
        String[] possiblePaths = {
            ".env",
            "backend/.env",
            "../.env",
            "../backend/.env"
        };
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("#") || line.isEmpty()) continue;
                        if (line.startsWith("MAPBOX_ACCESS_TOKEN=")) {
                            String val = line.substring("MAPBOX_ACCESS_TOKEN=".length()).trim();
                            val = val.replaceAll("^[\"']|[\"']$", "");
                            if (!val.isEmpty()) {
                                return val;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public void setMapboxAccessToken(String mapboxAccessToken) {
        this.mapboxAccessToken = mapboxAccessToken != null ? mapboxAccessToken.trim() : null;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public void setCountryFilter(String countryFilter) {
        this.countryFilter = countryFilter != null ? countryFilter.trim() : "";
    }

    // Built-in geographic catalog covering key transit, academic, and landmark hubs across India
    private static final List<PlaceSuggestionDto> KNOWN_HUBS = List.of(
            PlaceSuggestionDto.builder()
                    .placeId("vit-bhopal")
                    .mainText("VIT Bhopal University")
                    .secondaryText("Kothri Kalan, Sehore, Madhya Pradesh 466114, India")
                    .fullAddress("VIT Bhopal University, Bhopal-Indore Highway, Kothri Kalan, Sehore, Madhya Pradesh 466114")
                    .latitude(23.0775)
                    .longitude(76.8513)
                    .name("VIT Bhopal University")
                    .formattedAddress("VIT Bhopal University, Bhopal-Indore Highway, Kothri Kalan, Sehore, Madhya Pradesh 466114")
                    .city("Sehore")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("vit-bhopal")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("sehore-city")
                    .mainText("Sehore")
                    .secondaryText("Sehore, Madhya Pradesh 466001, India")
                    .fullAddress("Sehore, Madhya Pradesh 466001, India")
                    .latitude(23.2038)
                    .longitude(77.0844)
                    .name("Sehore")
                    .formattedAddress("Sehore, Madhya Pradesh 466001, India")
                    .city("Sehore")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("sehore-city")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("ind-airport")
                    .mainText("Indore Airport (Devi Ahilyabai Holkar)")
                    .secondaryText("Depalpur Road, Indore, Madhya Pradesh 452005, India")
                    .fullAddress("Devi Ahilyabai Holkar International Airport (IDR), Depalpur Road, Indore, Madhya Pradesh 452005")
                    .latitude(22.7217)
                    .longitude(75.8011)
                    .name("Indore Airport")
                    .formattedAddress("Devi Ahilyabai Holkar International Airport, Indore, Madhya Pradesh 452005")
                    .city("Indore")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("ind-airport")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("ind-rajwada")
                    .mainText("Rajwada Palace")
                    .secondaryText("MG Road, Rajwada, Indore, Madhya Pradesh 452002, India")
                    .fullAddress("Rajwada Palace, MG Road, Indore, Madhya Pradesh 452002")
                    .latitude(22.7186)
                    .longitude(75.8553)
                    .name("Rajwada Palace")
                    .formattedAddress("Rajwada Palace, MG Road, Indore, Madhya Pradesh 452002")
                    .city("Indore")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("ind-rajwada")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("bho-station")
                    .mainText("Bhopal Railway Station")
                    .secondaryText("Hamidia Road, Bhopal, Madhya Pradesh 462001, India")
                    .fullAddress("Bhopal Railway Station, Hamidia Road, Bhopal, Madhya Pradesh 462001")
                    .latitude(23.2599)
                    .longitude(77.4126)
                    .name("Bhopal Railway Station")
                    .formattedAddress("Bhopal Railway Station, Hamidia Road, Bhopal, Madhya Pradesh 462001")
                    .city("Bhopal")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("bho-station")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("bho-airport")
                    .mainText("Bhopal Airport (Raja Bhoj)")
                    .secondaryText("Gandhi Nagar, Bhopal, Madhya Pradesh 462036, India")
                    .fullAddress("Raja Bhoj Airport, Airport Road, Gandhi Nagar, Bhopal, Madhya Pradesh 462036")
                    .latitude(23.2875)
                    .longitude(77.3378)
                    .name("Bhopal Airport")
                    .formattedAddress("Raja Bhoj Airport, Gandhi Nagar, Bhopal, Madhya Pradesh 462036")
                    .city("Bhopal")
                    .state("Madhya Pradesh")
                    .country("India")
                    .providerPlaceId("bho-airport")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("del-ndls")
                    .mainText("New Delhi Railway Station")
                    .secondaryText("Paharganj, New Delhi, Delhi 110002, India")
                    .fullAddress("New Delhi Railway Station, Bhavbhuti Marg, Paharganj, New Delhi, Delhi 110002")
                    .latitude(28.6429)
                    .longitude(77.2195)
                    .name("New Delhi Railway Station")
                    .formattedAddress("New Delhi Railway Station, Bhavbhuti Marg, Paharganj, New Delhi, Delhi 110002")
                    .city("New Delhi")
                    .state("Delhi")
                    .country("India")
                    .providerPlaceId("del-ndls")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("del-cp")
                    .mainText("Connaught Place")
                    .secondaryText("Central Delhi, New Delhi, Delhi 110001, India")
                    .fullAddress("Connaught Place, Central Delhi, New Delhi, Delhi 110001")
                    .latitude(28.6315)
                    .longitude(77.2167)
                    .name("Connaught Place")
                    .formattedAddress("Connaught Place, Central Delhi, New Delhi, Delhi 110001")
                    .city("New Delhi")
                    .state("Delhi")
                    .country("India")
                    .providerPlaceId("del-cp")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("del-indiagate")
                    .mainText("India Gate")
                    .secondaryText("Kartavya Path, India Gate, New Delhi, Delhi 110001, India")
                    .fullAddress("India Gate, Kartavya Path, India Gate, New Delhi, Delhi 110001")
                    .latitude(28.6129)
                    .longitude(77.2295)
                    .name("India Gate")
                    .formattedAddress("India Gate, Kartavya Path, New Delhi, Delhi 110001")
                    .city("New Delhi")
                    .state("Delhi")
                    .country("India")
                    .providerPlaceId("del-indiagate")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("del-igi")
                    .mainText("Indira Gandhi International Airport (DEL)")
                    .secondaryText("Terminal 3, New Delhi, Delhi 110037, India")
                    .fullAddress("Indira Gandhi International Airport, New Delhi, Delhi 110037")
                    .latitude(28.5562)
                    .longitude(77.1000)
                    .name("Indira Gandhi International Airport")
                    .formattedAddress("Indira Gandhi International Airport, New Delhi, Delhi 110037")
                    .city("New Delhi")
                    .state("Delhi")
                    .country("India")
                    .providerPlaceId("del-igi")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("bom-csmia")
                    .mainText("Mumbai Airport (CSMIA)")
                    .secondaryText("Sahar, Andheri East, Mumbai, Maharashtra 400099, India")
                    .fullAddress("Chhatrapati Shivaji Maharaj International Airport (BOM), Mumbai, Maharashtra 400099")
                    .latitude(19.0896)
                    .longitude(72.8656)
                    .name("Mumbai Airport")
                    .formattedAddress("CSMIA Airport, Vile Parle East, Mumbai, Maharashtra 400099")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .country("India")
                    .providerPlaceId("bom-csmia")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("bom-gateway")
                    .mainText("Gateway of India")
                    .secondaryText("Apollo Bandar, Colaba, Mumbai, Maharashtra 400001, India")
                    .fullAddress("Gateway of India, Apollo Bandar, Colaba, Mumbai, Maharashtra 400001")
                    .latitude(18.9220)
                    .longitude(72.8347)
                    .name("Gateway of India")
                    .formattedAddress("Gateway of India, Apollo Bandar, Colaba, Mumbai, Maharashtra 400001")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .country("India")
                    .providerPlaceId("bom-gateway")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("bom-bkc")
                    .mainText("Bandra Kurla Complex (BKC)")
                    .secondaryText("Bandra East, Mumbai, Maharashtra 400051, India")
                    .fullAddress("Bandra Kurla Complex, Bandra East, Mumbai, Maharashtra 400051")
                    .latitude(19.0657)
                    .longitude(72.8687)
                    .name("Bandra Kurla Complex")
                    .formattedAddress("Bandra Kurla Complex, Bandra East, Mumbai, Maharashtra 400051")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .country("India")
                    .providerPlaceId("bom-bkc")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("blr-mgroad")
                    .mainText("MG Road Metro Station")
                    .secondaryText("Shivaji Nagar, Bengaluru, Karnataka 560001, India")
                    .fullAddress("MG Road, Shivaji Nagar, Bengaluru, Karnataka 560001")
                    .latitude(12.9756)
                    .longitude(77.6066)
                    .name("MG Road Metro Station")
                    .formattedAddress("MG Road, Shivaji Nagar, Bengaluru, Karnataka 560001")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .country("India")
                    .providerPlaceId("blr-mgroad")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("blr-kia")
                    .mainText("Kempegowda International Airport (BLR)")
                    .secondaryText("Devanahalli, Bengaluru, Karnataka 560300, India")
                    .fullAddress("Kempegowda International Airport, Devanahalli, Bengaluru, Karnataka 560300")
                    .latitude(13.1986)
                    .longitude(77.7066)
                    .name("Kempegowda International Airport")
                    .formattedAddress("Kempegowda International Airport, Devanahalli, Bengaluru, Karnataka 560300")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .country("India")
                    .providerPlaceId("blr-kia")
                    .build(),
            PlaceSuggestionDto.builder()
                    .placeId("del-cybercity")
                    .mainText("DLF Cyber City")
                    .secondaryText("DLF Phase 2, Gurugram, Haryana 122002, India")
                    .fullAddress("DLF Cyber City, Sector 24, Gurugram, Haryana 122002")
                    .latitude(28.4950)
                    .longitude(77.0895)
                    .name("DLF Cyber City")
                    .formattedAddress("DLF Cyber City, Sector 24, Gurugram, Haryana 122002")
                    .city("Gurugram")
                    .state("Haryana")
                    .country("India")
                    .providerPlaceId("del-cybercity")
                    .build()
    );

    @Override
    public String getProviderName() {
        return "Mapbox / OpenStreetMap Location Provider";
    }

    @Override
    public List<PlaceSuggestionDto> getPlaceSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String trimmedQuery = query.trim();

        // 1. Try Mapbox Geocoding v5 if token is configured
        if (isMapboxTokenValid()) {
            return queryMapboxGeocoding(trimmedQuery);
        }

        // 2. Multi-city intelligent keyword matching across known transport, academic & landmark hubs
        if (fallbackEnabled) {
            String lowerQuery = trimmedQuery.toLowerCase(Locale.ROOT);
            List<PlaceSuggestionDto> matched = KNOWN_HUBS.stream()
                    .filter(p -> p.getMainText().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || p.getSecondaryText().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || p.getFullAddress().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || lowerQuery.contains(p.getMainText().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());

            if (!matched.isEmpty()) {
                return matched;
            }
        }

        log.warn("Mapbox location search requested for '{}' but Mapbox access token is not configured.", trimmedQuery);
        throw new ProviderUnavailableException("Location search service is unavailable: Mapbox access token is not configured.");
    }

    @Override
    public LocationDto geocode(String address) {
        if (address == null || address.trim().isBlank()) {
            return null;
        }

        String trimmed = address.trim();

        // 1. Try Mapbox Geocoding if token available
        if (isMapboxTokenValid()) {
            List<PlaceSuggestionDto> mapboxResults = queryMapboxGeocoding(trimmed);
            if (mapboxResults != null && !mapboxResults.isEmpty()) {
                return toLocationDto(mapboxResults.get(0));
            }
            return null;
        }

        // 2. Match known hubs if fallback is enabled
        if (fallbackEnabled) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            for (PlaceSuggestionDto hub : KNOWN_HUBS) {
                String hubMain = hub.getMainText().toLowerCase(Locale.ROOT);
                if (hubMain.equals(lower) || lower.equals(hubMain)) {
                    return toLocationDto(hub);
                }
            }
            for (PlaceSuggestionDto hub : KNOWN_HUBS) {
                String hubMain = hub.getMainText().toLowerCase(Locale.ROOT);
                if (hubMain.contains(lower) || lower.contains(hubMain)) {
                    return toLocationDto(hub);
                }
            }
            for (PlaceSuggestionDto hub : KNOWN_HUBS) {
                if (hub.getFullAddress().toLowerCase(Locale.ROOT).contains(lower)) {
                    return toLocationDto(hub);
                }
            }
        }

        log.warn("Mapbox geocoding requested for '{}' but Mapbox access token is not configured.", trimmed);
        throw new ProviderUnavailableException("Location search service is unavailable: Mapbox access token is not configured.");
    }

    @Override
    public LocationDto reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required for reverse geocoding");
        }
        double lat = latitude;
        double lon = longitude;

        if (isMapboxTokenValid()) {
            try {
                String url = String.format(Locale.US,
                        "https://api.mapbox.com/geocoding/v5/mapbox.places/%f,%f.json?access_token=%s&limit=1",
                        lon, lat, mapboxAccessToken);
                String responseBody = restClient.get().uri(url).retrieve().body(String.class);
                if (responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode features = root.path("features");
                    if (features.isArray() && !features.isEmpty()) {
                        JsonNode first = features.get(0);
                        String placeName = first.path("place_name").asText(String.format(Locale.US, "Location (%.5f, %.5f)", lat, lon));
                        String name = first.path("text").asText(placeName.split(",")[0]);
                        String placeId = first.path("id").asText("");
                        return LocationDto.builder()
                                .name(name)
                                .formattedAddress(placeName)
                                .latitude(lat)
                                .longitude(lon)
                                .address(placeName)
                                .city(extractCity(first, name, placeId))
                                .state(extractState(first, name, placeId))
                                .country(extractCountry(first, placeId, placeName))
                                .providerPlaceId(!placeId.isEmpty() ? placeId : ("revgeo-" + Math.abs(placeName.hashCode())))
                                .build();
                    }
                }
            } catch (Exception e) {
                log.warn("Mapbox reverse-geocoding failed for [{}, {}]: {}", lat, lon, e.getMessage());
            }
        }

        // Proximity match against known hubs if fallback is enabled
        if (fallbackEnabled) {
            PlaceSuggestionDto closest = null;
            double minDistance = Double.MAX_VALUE;
            for (PlaceSuggestionDto hub : KNOWN_HUBS) {
                double d = Math.hypot(hub.getLatitude() - lat, hub.getLongitude() - lon);
                if (d < minDistance) {
                    minDistance = d;
                    closest = hub;
                }
            }

            if (closest != null && minDistance < 0.05) { // Within ~5km
                return toLocationDto(closest);
            }
        }

        String readableAddress = String.format(Locale.US, "Location (%.5f, %.5f)", lat, lon);
        return LocationDto.builder()
                .name("Current Coordinates")
                .formattedAddress(readableAddress)
                .latitude(lat)
                .longitude(lon)
                .address(readableAddress)
                .city("Detected Coordinates")
                .state("")
                .country("India")
                .providerPlaceId(String.format(Locale.US, "coords-%.5f-%.5f", lat, lon))
                .build();
    }

    @Override
    public RouteEstimateResponseDto calculateRoute(LocationDto pickup, LocationDto destination) {
        if (pickup == null || pickup.getLatitude() == null || pickup.getLongitude() == null) {
            throw new IllegalArgumentException("Pickup coordinates are required for route calculation");
        }
        if (destination == null || destination.getLatitude() == null || destination.getLongitude() == null) {
            throw new IllegalArgumentException("Destination coordinates are required for route calculation");
        }

        double pLat = pickup.getLatitude();
        double pLon = pickup.getLongitude();
        double dLat = destination.getLatitude();
        double dLon = destination.getLongitude();

        String pAddr = pickup.getFormattedAddress() != null ? pickup.getFormattedAddress() : pickup.getName();
        String dAddr = destination.getFormattedAddress() != null ? destination.getFormattedAddress() : destination.getName();

        // 1. Try Mapbox Directions API if token is configured
        if (isMapboxTokenValid()) {
            try {
                String directionsUrl = String.format(Locale.US,
                        "https://api.mapbox.com/directions/v5/mapbox/driving/%f,%f;%f,%f?access_token=%s&geometries=geojson&overview=full",
                        pLon, pLat, dLon, dLat, mapboxAccessToken);
                String resp = restClient.get().uri(directionsUrl).retrieve().body(String.class);
                if (resp != null) {
                    JsonNode root = objectMapper.readTree(resp);
                    JsonNode routes = root.path("routes");
                    if (routes.isArray() && !routes.isEmpty()) {
                        JsonNode route = routes.get(0);
                        double distanceMeters = route.path("distance").asDouble();
                        double durationSecs = route.path("duration").asDouble();
                        double roadKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
                        int durationMins = (int) Math.max(3, Math.round(durationSecs / 60.0));

                        List<List<Double>> polyline = new ArrayList<>();
                        JsonNode coords = route.path("geometry").path("coordinates");
                        if (coords.isArray()) {
                            for (JsonNode pt : coords) {
                                if (pt.isArray() && pt.size() >= 2) {
                                    polyline.add(List.of(pt.get(1).asDouble(), pt.get(0).asDouble())); // [lat, lon]
                                }
                            }
                        }

                        if (!polyline.isEmpty()) {
                            return RouteEstimateResponseDto.builder()
                                    .distanceKm(roadKm)
                                    .durationMinutes(durationMins)
                                    .pickupAddress(pAddr)
                                    .dropAddress(dAddr)
                                    .routeSource("MAPBOX")
                                    .polylineCoordinates(polyline)
                                    .build();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Mapbox directions route failed: {}. Falling back to dynamic coordinate router.", e.getMessage());
            }
        }

        // 2. Pure dynamic mathematical route calculation (Haversine distance * 1.35 road winding factor)
        double directDistance = haversineDistanceKm(pLat, pLon, dLat, dLon);
        double roadDistanceKm = Math.max(1.0, Math.round(directDistance * 1.35 * 10.0) / 10.0);
        int durationMins = (int) Math.max(3, Math.round(roadDistanceKm * 2.2)); // ~27 km/h urban/highway travel speed

        // Dynamic multi-step interpolated road polyline
        List<List<Double>> polyline = new ArrayList<>();
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double fraction = (double) i / steps;
            double lat = pLat + (dLat - pLat) * fraction;
            double lon = pLon + (dLon - pLon) * fraction;
            if (i > 0 && i < steps) {
                double curveOffset = Math.sin(fraction * Math.PI) * 0.006;
                lat += curveOffset;
                lon -= curveOffset * 0.4;
            }
            polyline.add(List.of(Math.round(lat * 10000.0) / 10000.0, Math.round(lon * 10000.0) / 10000.0));
        }

        return RouteEstimateResponseDto.builder()
                .distanceKm(roadDistanceKm)
                .durationMinutes(durationMins)
                .pickupAddress(pAddr)
                .dropAddress(dAddr)
                .routeSource("ESTIMATED")
                .polylineCoordinates(polyline)
                .build();
    }

    // ── Helper Methods ──

    public boolean isMapboxTokenValid() {
        return mapboxAccessToken != null
                && !mapboxAccessToken.isBlank()
                && (mapboxAccessToken.startsWith("pk.") || mapboxAccessToken.startsWith("sk."));
    }

    private List<PlaceSuggestionDto> queryMapboxGeocoding(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
        String countryParam = (countryFilter != null && !countryFilter.trim().isBlank())
                ? "&country=" + URLEncoder.encode(countryFilter.trim(), StandardCharsets.UTF_8)
                : "";
        String url = String.format(Locale.US,
                "https://api.mapbox.com/geocoding/v5/mapbox.places/%s.json?access_token=%s&autocomplete=true&limit=8%s",
                encoded, mapboxAccessToken.trim(), countryParam);

        String json;
        try {
            json = restClient.get().uri(url).retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("Mapbox authentication failed with status {}. Verify MAPBOX_ACCESS_TOKEN.", e.getStatusCode().value());
                throw new ProviderUnavailableException("Location search service is unavailable: invalid Mapbox access token.");
            }
            log.error("Mapbox API returned status {}: {}", e.getStatusCode().value(), e.getMessage());
            throw new ProviderUnavailableException("Location search is temporarily unavailable.");
        } catch (Exception e) {
            if (e instanceof ProviderUnavailableException) {
                throw (ProviderUnavailableException) e;
            }
            log.error("Mapbox geocoding call failed for query '{}': {}", query, e.getMessage());
            throw new ProviderUnavailableException("Location search is temporarily unavailable.");
        }

        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        return parseMapboxFeatures(json);
    }

    private List<PlaceSuggestionDto> parseMapboxFeatures(String json) {
        List<PlaceSuggestionDto> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode features = root.path("features");
            if (features.isArray()) {
                for (JsonNode f : features) {
                    JsonNode center = f.path("center");
                    if (center.isArray() && center.size() >= 2) {
                        double lon = center.get(0).asDouble();
                        double lat = center.get(1).asDouble();

                        String placeId = f.path("id").asText("");
                        String text = f.path("text").asText("").trim();
                        if (text.isEmpty()) {
                            text = f.path("matching_text").asText("").trim();
                        }
                        String placeName = f.path("place_name").asText("").trim();
                        if (placeName.isEmpty()) {
                            placeName = f.path("matching_place_name").asText("").trim();
                        }

                        String mainText = !text.isEmpty() ? text : placeName;
                        String formattedAddress = !placeName.isEmpty() ? placeName : mainText;

                        String secondaryText = "";
                        if (formattedAddress.startsWith(mainText + ", ")) {
                            secondaryText = formattedAddress.substring(mainText.length() + 2).trim();
                        } else if (!formattedAddress.equalsIgnoreCase(mainText)) {
                            secondaryText = formattedAddress;
                        }

                        String city = extractCity(f, text, placeId);
                        String state = extractState(f, text, placeId);
                        String country = extractCountry(f, placeId, formattedAddress);

                        results.add(PlaceSuggestionDto.builder()
                                .placeId(placeId)
                                .mainText(mainText)
                                .secondaryText(secondaryText)
                                .fullAddress(formattedAddress)
                                .latitude(lat)
                                .longitude(lon)
                                .name(mainText)
                                .formattedAddress(formattedAddress)
                                .city(city)
                                .state(state)
                                .country(country)
                                .providerPlaceId(placeId)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Mapbox JSON response: {}", e.getMessage());
            throw new ProviderUnavailableException("Location search is temporarily unavailable.");
        }
        return results;
    }

    private String extractCity(JsonNode feature, String text, String placeId) {
        JsonNode context = feature.path("context");
        if (context.isArray()) {
            for (JsonNode c : context) {
                String id = c.path("id").asText("");
                if (id.startsWith("place")) {
                    return c.path("text").asText("");
                }
            }
        }
        if (placeId.startsWith("place") || placeId.startsWith("locality")) {
            return text;
        }
        if (context.isArray()) {
            for (JsonNode c : context) {
                String id = c.path("id").asText("");
                if (id.startsWith("locality") || id.startsWith("district")) {
                    return c.path("text").asText("");
                }
            }
        }
        return "";
    }

    private String extractState(JsonNode feature, String text, String placeId) {
        if (placeId.startsWith("region")) {
            return text;
        }
        JsonNode context = feature.path("context");
        if (context.isArray()) {
            for (JsonNode c : context) {
                String id = c.path("id").asText("");
                if (id.startsWith("region")) {
                    return c.path("text").asText("");
                }
            }
        }
        return "";
    }

    private String extractCountry(JsonNode feature, String placeId, String formattedAddress) {
        if (placeId.startsWith("country")) {
            return feature.path("text").asText("India");
        }
        JsonNode context = feature.path("context");
        if (context.isArray()) {
            for (JsonNode c : context) {
                String id = c.path("id").asText("");
                if (id.startsWith("country")) {
                    return c.path("text").asText("India");
                }
            }
        }
        if (countryFilter != null && countryFilter.equalsIgnoreCase("in")) {
            return "India";
        }
        return (formattedAddress != null && formattedAddress.toLowerCase(Locale.ROOT).contains("india")) ? "India" : "";
    }

    private LocationDto toLocationDto(PlaceSuggestionDto p) {
        return LocationDto.builder()
                .name(p.getName())
                .formattedAddress(p.getFormattedAddress())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .address(p.getFormattedAddress())
                .city(p.getCity() != null ? p.getCity() : "")
                .state(p.getState() != null ? p.getState() : "")
                .country(p.getCountry() != null ? p.getCountry() : "India")
                .providerPlaceId(p.getProviderPlaceId())
                .build();
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
