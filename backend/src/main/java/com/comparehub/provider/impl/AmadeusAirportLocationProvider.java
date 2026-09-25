package com.comparehub.provider.impl;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.AirportLocationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AmadeusAirportLocationProvider implements AirportLocationProvider {

    @Value("${app.providers.amadeus.enabled:true}")
    private boolean enabled = true;

    @Value("${app.providers.amadeus.client-id:}")
    private String clientId;

    @Value("${app.providers.amadeus.client-secret:}")
    private String clientSecret;

    @Value("${app.providers.amadeus.auth-url:https://test.api.amadeus.com/v1/security/oauth2/token}")
    private String authUrl = "https://test.api.amadeus.com/v1/security/oauth2/token";

    @Value("${app.providers.amadeus.location-url:https://test.api.amadeus.com/v1/reference-data/locations}")
    private String locationUrl = "https://test.api.amadeus.com/v1/reference-data/locations";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // In-memory token cache
    private String cachedToken;
    private Instant tokenExpiry = Instant.MIN;

    public AmadeusAirportLocationProvider() {
        this(RestClient.builder().build(), new ObjectMapper());
    }

    public AmadeusAirportLocationProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient != null ? restClient : RestClient.builder().build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    // Setters for testing
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setLocationUrl(String locationUrl) {
        this.locationUrl = locationUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    @Override
    public String getProviderName() {
        return "Amadeus Airport & City Search";
    }

    @Override
    public boolean isAvailable() {
        return enabled && clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    @Override
    public List<AirportResultDto> searchAirports(String query, int limit) {
        if (!isAvailable() || query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String trimmed = query.trim();
        int maxLimit = limit > 0 ? limit : 10;

        try {
            String token = getAccessToken();
            if (token == null) {
                log.warn("Amadeus access token unavailable, skipping external airport search for '{}'", trimmed);
                return Collections.emptyList();
            }

            String encodedKeyword = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
            String url = locationUrl + "?subType=AIRPORT,CITY&keyword=" + encodedKeyword + "&page[limit]=" + maxLimit;

            String responseJson = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                return Collections.emptyList();
            }

            return parseLocations(responseJson);
        } catch (Exception e) {
            log.warn("Amadeus airport lookup error for query '{}': {}", trimmed, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<AirportResultDto> parseLocations(String json) {
        List<AirportResultDto> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                return results;
            }

            for (JsonNode item : data) {
                String iataCode = item.path("iataCode").asText(null);
                if (iataCode == null || iataCode.isBlank()) {
                    continue;
                }

                String name = item.path("name").asText(iataCode);
                String subType = item.path("subType").asText("AIRPORT");
                JsonNode address = item.path("address");
                String cityName = address.path("cityName").asText(name);
                String countryName = address.path("countryName").asText("International");

                JsonNode geoCode = item.path("geoCode");
                Double lat = geoCode.has("latitude") ? geoCode.path("latitude").asDouble() : null;
                Double lon = geoCode.has("longitude") ? geoCode.path("longitude").asDouble() : null;

                // Format display name e.g. "LHR — London Heathrow Airport, London"
                String displayName = iataCode.toUpperCase() + " — " + name + ", " + cityName;

                results.add(AirportResultDto.builder()
                        .name(name)
                        .iataCode(iataCode.toUpperCase())
                        .cityName(cityName)
                        .countryName(countryName)
                        .airportType(subType)
                        .latitude(lat)
                        .longitude(lon)
                        .displayName(displayName)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse Amadeus location JSON response: {}", e.getMessage());
        }
        return results;
    }

    private synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        try {
            String form = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
            String response = restClient.post()
                    .uri(authUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode node = objectMapper.readTree(response);
                String token = node.path("access_token").asText(null);
                int expiresIn = node.path("expires_in").asInt(1799); // default 30 mins
                if (token != null) {
                    this.cachedToken = token;
                    this.tokenExpiry = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
                    return token;
                }
            }
        } catch (Exception e) {
            log.warn("Amadeus OAuth2 authentication failed: {}", e.getMessage());
        }
        return null;
    }
}
