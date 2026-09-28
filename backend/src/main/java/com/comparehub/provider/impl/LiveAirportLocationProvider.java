package com.comparehub.provider.impl;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.AirportLocationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic airport and city location provider connecting to live aviation places API.
 * Provides real-time world-wide airport discovery (IATA codes, cities, countries, coordinates).
 */
@Slf4j
@Component
@Order(2)
public class LiveAirportLocationProvider implements AirportLocationProvider {

    @Value("${app.providers.airport-location.enabled:true}")
    private boolean enabled = true;

    @Value("${app.providers.airport-location.api-url:https://autocomplete.travelpayouts.com/places2}")
    private String apiUrl = "https://autocomplete.travelpayouts.com/places2";

    @Value("${app.providers.airport-location.timeout-ms:3500}")
    private int timeoutMs = 3500;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LiveAirportLocationProvider() {
        this(createDefaultRestClient(3500), new ObjectMapper());
    }

    public LiveAirportLocationProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient != null ? restClient : createDefaultRestClient(3500);
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private static RestClient createDefaultRestClient(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory).build();
    }

    // Setters for testing/configuration
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String getProviderName() {
        return "Travelpayouts Aviation Places API";
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiUrl != null && !apiUrl.isBlank();
    }

    @Override
    public List<AirportResultDto> searchAirports(String query, int limit) {
        if (!isAvailable() || query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String trimmed = query.trim();
        int maxLimit = limit > 0 ? limit : 10;

        try {
            String encodedTerm = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
            String url = apiUrl + "?locale=en&types[]=airport&types[]=city&term=" + encodedTerm;

            log.debug("Querying live airport provider for '{}': {}", trimmed, url);

            String responseJson = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                return Collections.emptyList();
            }

            return parseAndRankLocations(responseJson, trimmed, maxLimit);
        } catch (Exception e) {
            log.warn("Live airport location lookup failed for '{}': {}", trimmed, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<AirportResultDto> parseAndRankLocations(String json, String rawQuery, int limit) {
        List<AirportResultDto> rawResults = new ArrayList<>();
        Map<AirportResultDto, Integer> scored = new LinkedHashMap<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return Collections.emptyList();
            }

            String qLower = rawQuery.toLowerCase(Locale.ROOT);

            for (JsonNode item : root) {
                String code = item.path("code").asText(null);
                if (code == null || code.isBlank()) {
                    continue;
                }
                code = code.trim().toUpperCase(Locale.ROOT);

                String id = item.path("id").asText(code);
                String rawType = item.path("type").asText("airport");
                boolean isCity = "city".equalsIgnoreCase(rawType);
                String typeStr = isCity ? "CITY" : "AIRPORT";

                String name = item.path("name").asText(code);
                String cityName = item.path("city_name").asText(null);
                if (cityName == null || cityName.isBlank()) {
                    cityName = isCity ? name : name;
                }

                String countryName = item.path("country_name").asText("International");
                String countryCode = item.path("country_code").asText("").toUpperCase(Locale.ROOT);
                String mainAirportName = item.path("main_airport_name").asText(null);

                // For a city with a known main airport, use it if helpful
                String resolvedName = name;
                if (isCity && mainAirportName != null && !mainAirportName.isBlank()) {
                    resolvedName = mainAirportName;
                }

                Double lat = null;
                Double lon = null;
                JsonNode coords = item.path("coordinates");
                if (coords.isObject()) {
                    if (coords.has("lat") && coords.path("lat").isNumber()) {
                        lat = coords.path("lat").asDouble();
                    }
                    if (coords.has("lon") && coords.path("lon").isNumber()) {
                        lon = coords.path("lon").asDouble();
                    }
                }

                // Filter check: must meaningfully relate to query keyword
                String codeLower = code.toLowerCase(Locale.ROOT);
                String cityLower = cityName.toLowerCase(Locale.ROOT);
                String nameLower = resolvedName.toLowerCase(Locale.ROOT);
                String countryLower = countryName.toLowerCase(Locale.ROOT);
                String mainLower = mainAirportName != null ? mainAirportName.toLowerCase(Locale.ROOT) : "";

                boolean matches = codeLower.equals(qLower)
                        || codeLower.startsWith(qLower)
                        || cityLower.contains(qLower)
                        || nameLower.contains(qLower)
                        || countryLower.contains(qLower)
                        || countryCode.equalsIgnoreCase(rawQuery)
                        || mainLower.contains(qLower);

                if (!matches) {
                    continue;
                }

                // Display name format:
                // City: "London, United Kingdom — All Airports (LON)"
                // Airport: "Delhi — Indira Gandhi International Airport — DEL"
                String displayName;
                if (isCity && (mainAirportName == null || mainAirportName.isBlank())) {
                    displayName = cityName + (countryName != null && !countryName.isBlank() ? ", " + countryName : "")
                            + " — All Airports (" + code + ")";
                } else if (!cityName.equalsIgnoreCase(resolvedName)) {
                    displayName = cityName + " — " + resolvedName + " — " + code;
                } else {
                    displayName = resolvedName + " — " + code;
                }

                AirportResultDto dto = AirportResultDto.builder()
                        .id(id)
                        .name(resolvedName)
                        .iataCode(code)
                        .cityName(cityName)
                        .countryName(countryName)
                        .countryCode(countryCode)
                        .airportType(typeStr)
                        .type(typeStr)
                        .latitude(lat)
                        .longitude(lon)
                        .displayName(displayName)
                        .build();

                int score = calculateMatchScore(codeLower, cityLower, nameLower, countryLower, qLower, item.path("weight").asInt(0));
                scored.put(dto, score);
            }

            return scored.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .map(Map.Entry::getKey)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error parsing live airport response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private int calculateMatchScore(String codeLower, String cityLower, String nameLower, String countryLower, String qLower, int weight) {
        int score = 0;

        // Exact IATA match
        if (codeLower.equals(qLower)) {
            score += 1000;
        } else if (codeLower.startsWith(qLower)) {
            score += 800;
        }

        // Exact city match
        if (cityLower.equals(qLower)) {
            score += 900;
        } else if (cityLower.startsWith(qLower)) {
            score += 700;
        } else if (cityLower.contains(qLower)) {
            score += 500;
        }

        // Airport name
        if (nameLower.equals(qLower)) {
            score += 850;
        } else if (nameLower.startsWith(qLower)) {
            score += 650;
        } else if (nameLower.contains(qLower)) {
            score += 450;
        }

        // Country
        if (countryLower.startsWith(qLower)) {
            score += 200;
        }

        // API traffic weight boost (normalized up to 100)
        score += Math.min(100, weight / 1000);

        return score;
    }
}
