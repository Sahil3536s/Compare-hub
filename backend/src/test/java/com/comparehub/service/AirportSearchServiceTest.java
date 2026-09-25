package com.comparehub.service;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.provider.AirportLocationProvider;
import com.comparehub.provider.impl.AmadeusAirportLocationProvider;
import com.comparehub.service.impl.AirportSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AirportSearchServiceTest {

    private AirportLocationProvider mockProvider;
    private AirportSearchService airportSearchService;

    @BeforeEach
    void setUp() {
        mockProvider = mock(AirportLocationProvider.class);
        // By default provider is available
        when(mockProvider.isAvailable()).thenReturn(true);
        when(mockProvider.getProviderName()).thenReturn("Mock Airport Provider");
        airportSearchService = new AirportSearchServiceImpl(mockProvider);
    }

    @Test
    @DisplayName("Provider-backed Dynamic Search: queries external provider as primary authority")
    void testProviderBackedPrimarySearch() {
        AirportResultDto mockDto = AirportResultDto.builder()
                .iataCode("DEL")
                .name("Indira Gandhi International Airport")
                .cityName("Delhi")
                .countryName("India")
                .airportType("AIRPORT")
                .latitude(28.5562)
                .longitude(77.1000)
                .displayName("DEL — Indira Gandhi International Airport, Delhi")
                .build();

        when(mockProvider.searchAirports("del", 10)).thenReturn(List.of(mockDto));

        List<AirportResultDto> results = airportSearchService.searchAirports("del");
        assertFalse(results.isEmpty());
        assertEquals("DEL", results.get(0).getIataCode());
        assertEquals("Delhi", results.get(0).getCityName());
        verify(mockProvider, times(1)).searchAirports("del", 10);
    }

    @Test
    @DisplayName("Search by 3-Letter IATA Code: DEL, BHO, JFK, LHR, DXB")
    void testSearchByIataCode() {
        // Fallback test mode when provider is not configured/offline
        when(mockProvider.isAvailable()).thenReturn(false);

        List<AirportResultDto> del = airportSearchService.searchAirports("DEL");
        assertFalse(del.isEmpty());
        assertEquals("DEL", del.get(0).getIataCode());

        List<AirportResultDto> bho = airportSearchService.searchAirports("BHO");
        assertFalse(bho.isEmpty());
        assertEquals("BHO", bho.get(0).getIataCode());

        List<AirportResultDto> jfk = airportSearchService.searchAirports("JFK");
        assertFalse(jfk.isEmpty());
        assertEquals("JFK", jfk.get(0).getIataCode());

        List<AirportResultDto> lhr = airportSearchService.searchAirports("LHR");
        assertFalse(lhr.isEmpty());
        assertEquals("LHR", lhr.get(0).getIataCode());

        List<AirportResultDto> dxb = airportSearchService.searchAirports("DXB");
        assertFalse(dxb.isEmpty());
        assertEquals("DXB", dxb.get(0).getIataCode());
    }

    @Test
    @DisplayName("Search by Full City Name: Delhi, Bhopal, Indore, Dubai, London, New York")
    void testSearchByCityName() {
        when(mockProvider.isAvailable()).thenReturn(false);

        List<AirportResultDto> delhi = airportSearchService.searchAirports("Delhi");
        assertFalse(delhi.isEmpty());
        assertEquals("DEL", delhi.get(0).getIataCode());

        List<AirportResultDto> bhopal = airportSearchService.searchAirports("Bhopal");
        assertFalse(bhopal.isEmpty());
        assertEquals("BHO", bhopal.get(0).getIataCode());

        List<AirportResultDto> indore = airportSearchService.searchAirports("Indore");
        assertFalse(indore.isEmpty());
        assertEquals("IDR", indore.get(0).getIataCode());

        List<AirportResultDto> dubai = airportSearchService.searchAirports("Dubai");
        assertFalse(dubai.isEmpty());
        assertEquals("DXB", dubai.get(0).getIataCode());
    }

    @Test
    @DisplayName("Search by Airport Name: Indira Gandhi, Raja Bhoj, Heathrow, John F. Kennedy")
    void testSearchByAirportName() {
        when(mockProvider.isAvailable()).thenReturn(false);

        List<AirportResultDto> indira = airportSearchService.searchAirports("Indira Gandhi");
        assertFalse(indira.isEmpty());
        assertEquals("DEL", indira.get(0).getIataCode());

        List<AirportResultDto> raja = airportSearchService.searchAirports("Raja Bhoj");
        assertFalse(raja.isEmpty());
        assertEquals("BHO", raja.get(0).getIataCode());

        List<AirportResultDto> heathrow = airportSearchService.searchAirports("Heathrow");
        assertFalse(heathrow.isEmpty());
        assertEquals("LHR", heathrow.get(0).getIataCode());
    }

    @Test
    @DisplayName("Search by Partial Text: 'Indi', 'Lon', 'Bho'")
    void testSearchByPartialText() {
        when(mockProvider.isAvailable()).thenReturn(false);

        List<AirportResultDto> indiResults = airportSearchService.searchAirports("Indi");
        assertFalse(indiResults.isEmpty());
        assertTrue(indiResults.stream().anyMatch(a -> a.getIataCode().equals("DEL") || a.getIataCode().equals("IDR")));

        List<AirportResultDto> lonResults = airportSearchService.searchAirports("Lon");
        assertFalse(lonResults.isEmpty());
        assertTrue(lonResults.stream().anyMatch(a -> a.getIataCode().equals("LHR")));

        List<AirportResultDto> bhoResults = airportSearchService.searchAirports("Bho");
        assertFalse(bhoResults.isEmpty());
        assertEquals("BHO", bhoResults.get(0).getIataCode());
    }

    @Test
    @DisplayName("Multiple-Airport Cities: London returns LHR, LGW, STN; New York returns JFK, LGA, EWR")
    void testMultipleAirportCities() {
        when(mockProvider.isAvailable()).thenReturn(false);

        // London
        List<AirportResultDto> lonResults = airportSearchService.searchAirports("London");
        assertTrue(lonResults.size() >= 3, "London must return multiple commercial airports");
        List<String> lonCodes = lonResults.stream().map(AirportResultDto::getIataCode).toList();
        assertTrue(lonCodes.contains("LHR"), "Must include Heathrow");
        assertTrue(lonCodes.contains("LGW"), "Must include Gatwick");
        assertTrue(lonCodes.contains("STN"), "Must include Stansted");

        // New York
        List<AirportResultDto> nyResults = airportSearchService.searchAirports("New York");
        assertTrue(nyResults.size() >= 3, "New York must return multiple commercial airports");
        List<String> nyCodes = nyResults.stream().map(AirportResultDto::getIataCode).toList();
        assertTrue(nyCodes.contains("JFK"), "Must include JFK");
        assertTrue(nyCodes.contains("LGA"), "Must include LaGuardia");
        assertTrue(nyCodes.contains("EWR"), "Must include Newark");
    }

    @Test
    @DisplayName("Structured Normalized Model: verifies all 8 contract fields are populated")
    void testNormalizedModelFields() {
        when(mockProvider.isAvailable()).thenReturn(false);

        AirportResultDto airport = airportSearchService.getAirportByIataCode("DEL");
        assertNotNull(airport);
        assertEquals("DEL", airport.getIataCode());
        assertEquals("Indira Gandhi International Airport", airport.getName());
        assertEquals("Delhi", airport.getCityName());
        assertEquals("India", airport.getCountryName());
        assertEquals("AIRPORT", airport.getAirportType());
        assertNotNull(airport.getLatitude());
        assertNotNull(airport.getLongitude());
        assertTrue(airport.getDisplayName().contains("DEL"));
    }

    @Test
    @DisplayName("Offline Resilience Fallback: falls back to demo list when provider throws exception")
    void testProviderExceptionFallback() {
        when(mockProvider.searchAirports(anyString(), anyInt())).thenThrow(new RuntimeException("API connection timeout"));

        List<AirportResultDto> results = airportSearchService.searchAirports("Delhi");
        assertFalse(results.isEmpty(), "Should fall back gracefully without propagating exception");
        assertEquals("DEL", results.get(0).getIataCode());
    }

    @Test
    @DisplayName("Edge Cases: null, blank, and single character queries return empty list")
    void testNullOrBlankQuery() {
        assertTrue(airportSearchService.searchAirports(null).isEmpty());
        assertTrue(airportSearchService.searchAirports("").isEmpty());
        assertTrue(airportSearchService.searchAirports("   ").isEmpty());
        assertTrue(airportSearchService.searchAirports("D").isEmpty());
    }

    @Test
    @DisplayName("Unknown Location: returns empty list for nonsensical queries")
    void testUnknownLocation() {
        when(mockProvider.isAvailable()).thenReturn(false);
        List<AirportResultDto> results = airportSearchService.searchAirports("NonexistentXYZ999");
        assertTrue(results.isEmpty());
    }
}
