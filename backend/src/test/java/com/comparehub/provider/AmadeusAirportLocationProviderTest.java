package com.comparehub.provider;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.impl.AmadeusAirportLocationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class AmadeusAirportLocationProviderTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private AmadeusAirportLocationProvider provider;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();

        provider = new AmadeusAirportLocationProvider(restClientBuilder.build(), objectMapper);
        provider.setEnabled(true);
        provider.setClientId("test-client-id");
        provider.setClientSecret("test-client-secret");
        provider.setAuthUrl("https://test.api.amadeus.com/v1/security/oauth2/token");
        provider.setLocationUrl("https://test.api.amadeus.com/v1/reference-data/locations");
    }

    @Test
    @DisplayName("Availability check: returns false when credentials are empty")
    void testAvailability() {
        assertTrue(provider.isAvailable());

        AmadeusAirportLocationProvider unconfigured = new AmadeusAirportLocationProvider();
        unconfigured.setClientId("");
        unconfigured.setClientSecret("");
        assertFalse(unconfigured.isAvailable());
    }

    @Test
    @DisplayName("Queries < 2 characters return empty list without network call")
    void testShortQuery() {
        assertTrue(provider.searchAirports("D", 10).isEmpty());
        assertTrue(provider.searchAirports(null, 10).isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Multi-Airport Search: fetches token and parses multiple airports (e.g. London -> LHR, LGW)")
    void testSearchMultipleAirports() {
        // 1. Mock OAuth token response
        String tokenJson = "{\"access_token\":\"mock-token-xyz\",\"expires_in\":1799}";
        mockServer.expect(requestTo("https://test.api.amadeus.com/v1/security/oauth2/token"))
                .andRespond(withSuccess(tokenJson, APPLICATION_JSON));

        // 2. Mock Location API response with multiple airports
        String locationJson = """
                {
                  "data": [
                    {
                      "type": "location",
                      "subType": "AIRPORT",
                      "name": "HEATHROW",
                      "detailedName": "LONDON/GB:HEATHROW",
                      "iataCode": "LHR",
                      "geoCode": {
                        "latitude": 51.4775,
                        "longitude": -0.4614
                      },
                      "address": {
                        "cityName": "LONDON",
                        "countryName": "UNITED KINGDOM"
                      }
                    },
                    {
                      "type": "location",
                      "subType": "AIRPORT",
                      "name": "GATWICK",
                      "detailedName": "LONDON/GB:GATWICK",
                      "iataCode": "LGW",
                      "geoCode": {
                        "latitude": 51.1561,
                        "longitude": -0.1781
                      },
                      "address": {
                        "cityName": "LONDON",
                        "countryName": "UNITED KINGDOM"
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://test.api.amadeus.com/v1/reference-data/locations?subType=AIRPORT,CITY&keyword=London&page%5Blimit%5D=10"))
                .andExpect(header("Authorization", "Bearer mock-token-xyz"))
                .andRespond(withSuccess(locationJson, APPLICATION_JSON));

        List<AirportResultDto> results = provider.searchAirports("London", 10);
        assertNotNull(results);
        assertEquals(2, results.size());

        AirportResultDto lhr = results.get(0);
        assertEquals("LHR", lhr.getIataCode());
        assertEquals("HEATHROW", lhr.getName());
        assertEquals("LONDON", lhr.getCityName());
        assertEquals("UNITED KINGDOM", lhr.getCountryName());
        assertEquals(51.4775, lhr.getLatitude());
        assertEquals(-0.4614, lhr.getLongitude());
        assertTrue(lhr.getDisplayName().contains("LHR"));

        AirportResultDto lgw = results.get(1);
        assertEquals("LGW", lgw.getIataCode());
        assertEquals("GATWICK", lgw.getName());

        mockServer.verify();
    }
}
