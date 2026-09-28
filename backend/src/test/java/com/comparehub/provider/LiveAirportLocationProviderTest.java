package com.comparehub.provider;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.impl.LiveAirportLocationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LiveAirportLocationProviderTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private LiveAirportLocationProvider provider;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();

        provider = new LiveAirportLocationProvider(restClientBuilder.build(), objectMapper);
        provider.setEnabled(true);
        provider.setApiUrl("https://autocomplete.travelpayouts.com/places2");
    }

    @Test
    @DisplayName("Availability check: returns true when enabled, false when disabled")
    void testAvailability() {
        assertTrue(provider.isAvailable());

        provider.setEnabled(false);
        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("Short queries (< 2 chars) return empty list without calling network")
    void testShortQuery() {
        assertTrue(provider.searchAirports("D", 10).isEmpty());
        assertTrue(provider.searchAirports(null, 10).isEmpty());
        assertTrue(provider.searchAirports("  ", 10).isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Dynamic Airport & City Search: 'del' returns multiple Delhi suggestions")
    void testSearchDelhiMultipleLocations() {
        String mockJson = """
                [
                  {
                    "id": "c715320f-fe86-4345-bf8e-ae87d954457b",
                    "type": "airport",
                    "code": "DEL",
                    "name": "Indira Gandhi International Airport",
                    "country_code": "IN",
                    "country_name": "India",
                    "city_code": "DEL",
                    "city_name": "Delhi",
                    "coordinates": {
                      "lon": 77.10079,
                      "lat": 28.556555
                    },
                    "weight": 17422
                  },
                  {
                    "id": "04345da5-e7a9-4cf2-8810-7fe629658a81",
                    "type": "city",
                    "code": "DEL",
                    "name": "Delhi",
                    "country_code": "IN",
                    "country_name": "India",
                    "coordinates": {
                      "lon": 77.2090212,
                      "lat": 28.6139391
                    },
                    "weight": 25357
                  },
                  {
                    "id": "19f11279-1c4a-47a3-b199-ea8da132c871",
                    "type": "airport",
                    "code": "QAH",
                    "name": "Hindon Airport",
                    "country_code": "IN",
                    "country_name": "India",
                    "city_code": "DEL",
                    "city_name": "Delhi",
                    "coordinates": {
                      "lon": 77.342137,
                      "lat": 28.70579
                    },
                    "weight": 1
                  }
                ]
                """;

        mockServer.expect(requestTo("https://autocomplete.travelpayouts.com/places2?locale=en&types%5B%5D=airport&types%5B%5D=city&term=del"))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        List<AirportResultDto> results = provider.searchAirports("del", 10);
        assertNotNull(results);
        assertEquals(3, results.size(), "Must return all matching Delhi airport/city options");

        AirportResultDto top = results.get(0);
        assertEquals("DEL", top.getIataCode());
        assertEquals("IN", top.getCountryCode());
        assertEquals("India", top.getCountryName());
        assertEquals("Delhi", top.getCityName());
        assertNotNull(top.getLatitude());
        assertNotNull(top.getLongitude());

        mockServer.verify();
    }

    @Test
    @DisplayName("Multiple-Airport Cities: 'london' returns multiple airports (LHR, LGW, LTN)")
    void testSearchLondonAirports() {
        String mockJson = """
                [
                  {
                    "id": "a6ec1def-4995-4a98-8c93-46b42b10fe7e",
                    "type": "city",
                    "code": "LON",
                    "name": "London",
                    "country_code": "GB",
                    "country_name": "United Kingdom",
                    "coordinates": {
                      "lon": -0.1277583,
                      "lat": 51.5073509
                    },
                    "weight": 162782
                  },
                  {
                    "id": "98d5fa02-8fc2-498c-b288-151f3a504697",
                    "type": "airport",
                    "code": "LHR",
                    "name": "London Heathrow Airport",
                    "country_code": "GB",
                    "country_name": "United Kingdom",
                    "city_code": "LON",
                    "city_name": "London",
                    "coordinates": {
                      "lon": -0.453566,
                      "lat": 51.469604
                    },
                    "weight": 51073
                  },
                  {
                    "id": "231e1e17-6648-48bb-b315-8a60855917fa",
                    "type": "airport",
                    "code": "LGW",
                    "name": "London Gatwick Airport",
                    "country_code": "GB",
                    "country_name": "United Kingdom",
                    "city_code": "LON",
                    "city_name": "London",
                    "coordinates": {
                      "lon": -0.161863,
                      "lat": 51.156807
                    },
                    "weight": 24308
                  }
                ]
                """;

        mockServer.expect(requestTo("https://autocomplete.travelpayouts.com/places2?locale=en&types%5B%5D=airport&types%5B%5D=city&term=london"))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        List<AirportResultDto> results = provider.searchAirports("london", 10);
        assertNotNull(results);
        assertEquals(3, results.size());

        List<String> codes = results.stream().map(AirportResultDto::getIataCode).toList();
        assertTrue(codes.contains("LON"));
        assertTrue(codes.contains("LHR"));
        assertTrue(codes.contains("LGW"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Nonexistent query returns empty list")
    void testNonexistentQuery() {
        mockServer.expect(requestTo("https://autocomplete.travelpayouts.com/places2?locale=en&types%5B%5D=airport&types%5B%5D=city&term=xyzunknownplace123"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<AirportResultDto> results = provider.searchAirports("xyzunknownplace123", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        mockServer.verify();
    }

    @Test
    @DisplayName("Graceful recovery on provider network error")
    void testProviderErrorGracefulRecovery() {
        mockServer.expect(requestTo("https://autocomplete.travelpayouts.com/places2?locale=en&types%5B%5D=airport&types%5B%5D=city&term=timeoutquery"))
                .andRespond(withServerError());

        List<AirportResultDto> results = provider.searchAirports("timeoutquery", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Must return empty list on network or server error without throwing");

        mockServer.verify();
    }
}
