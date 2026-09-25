package com.comparehub.service;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.service.impl.AirportSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AirportSearchServiceTest {

    private AirportSearchService airportSearchService;

    @BeforeEach
    void setUp() {
        airportSearchService = new AirportSearchServiceImpl();
    }

    @Test
    @DisplayName("Search by City: Delhi, Bhopal, Mumbai, New York, London, Indore, Bangalore, Dubai")
    void testSearchByCityName() {
        // Delhi
        List<AirportResultDto> delhiResults = airportSearchService.searchAirports("Delhi");
        assertFalse(delhiResults.isEmpty(), "Delhi should return airport results");
        assertEquals("DEL", delhiResults.get(0).getIataCode());
        assertEquals("Delhi", delhiResults.get(0).getCityName());

        // Bhopal
        List<AirportResultDto> bhopalResults = airportSearchService.searchAirports("Bhopal");
        assertFalse(bhopalResults.isEmpty(), "Bhopal should return airport results");
        assertEquals("BHO", bhopalResults.get(0).getIataCode());

        // Mumbai
        List<AirportResultDto> mumbaiResults = airportSearchService.searchAirports("Mumbai");
        assertFalse(mumbaiResults.isEmpty(), "Mumbai should return airport results");
        assertEquals("BOM", mumbaiResults.get(0).getIataCode());

        // Indore
        List<AirportResultDto> indoreResults = airportSearchService.searchAirports("Indore");
        assertFalse(indoreResults.isEmpty(), "Indore should return airport results");
        assertEquals("IDR", indoreResults.get(0).getIataCode());

        // Bangalore
        List<AirportResultDto> blrResults = airportSearchService.searchAirports("Bangalore");
        assertFalse(blrResults.isEmpty(), "Bangalore should return airport results");
        assertEquals("BLR", blrResults.get(0).getIataCode());

        // Dubai
        List<AirportResultDto> dxbResults = airportSearchService.searchAirports("Dubai");
        assertFalse(dxbResults.isEmpty(), "Dubai should return airport results");
        assertEquals("DXB", dxbResults.get(0).getIataCode());
    }

    @Test
    @DisplayName("Search by 3-Letter IATA Code: DEL, BOM, BHO, JFK, LHR, DXB, LAX")
    void testSearchByIataCode() {
        List<AirportResultDto> del = airportSearchService.searchAirports("DEL");
        assertEquals("DEL", del.get(0).getIataCode());

        List<AirportResultDto> jfk = airportSearchService.searchAirports("JFK");
        assertEquals("JFK", jfk.get(0).getIataCode());

        List<AirportResultDto> lhr = airportSearchService.searchAirports("LHR");
        assertEquals("LHR", lhr.get(0).getIataCode());

        List<AirportResultDto> dxb = airportSearchService.searchAirports("DXB");
        assertEquals("DXB", dxb.get(0).getIataCode());

        List<AirportResultDto> lax = airportSearchService.searchAirports("LAX");
        assertEquals("LAX", lax.get(0).getIataCode());
    }

    @Test
    @DisplayName("Search by Airport Name: Indira Gandhi, Raja Bhoj, Heathrow, John F Kennedy")
    void testSearchByAirportName() {
        List<AirportResultDto> indira = airportSearchService.searchAirports("Indira Gandhi");
        assertFalse(indira.isEmpty());
        assertEquals("DEL", indira.get(0).getIataCode());

        List<AirportResultDto> raja = airportSearchService.searchAirports("Raja Bhoj");
        assertFalse(raja.isEmpty());
        assertEquals("BHO", raja.get(0).getIataCode());

        List<AirportResultDto> heathrow = airportSearchService.searchAirports("Heathrow");
        assertFalse(heathrow.isEmpty());
        assertEquals("LHR", heathrow.get(0).getIataCode());

        List<AirportResultDto> kennedy = airportSearchService.searchAirports("John F. Kennedy");
        assertFalse(kennedy.isEmpty());
        assertEquals("JFK", kennedy.get(0).getIataCode());
    }

    @Test
    @DisplayName("Multiple-Airport Cities: New York returns JFK, LGA, EWR; London returns LHR, LGW, STN")
    void testMultipleAirportCities() {
        // New York
        List<AirportResultDto> nyResults = airportSearchService.searchAirports("New York");
        assertTrue(nyResults.size() >= 3, "New York must return multiple airports (JFK, LGA, EWR)");
        List<String> nyCodes = nyResults.stream().map(AirportResultDto::getIataCode).toList();
        assertTrue(nyCodes.contains("JFK"), "Must contain JFK");
        assertTrue(nyCodes.contains("LGA"), "Must contain LGA");
        assertTrue(nyCodes.contains("EWR"), "Must contain EWR");

        // London
        List<AirportResultDto> lonResults = airportSearchService.searchAirports("London");
        assertTrue(lonResults.size() >= 3, "London must return multiple airports (LHR, LGW, STN, etc.)");
        List<String> lonCodes = lonResults.stream().map(AirportResultDto::getIataCode).toList();
        assertTrue(lonCodes.contains("LHR"), "Must contain LHR");
        assertTrue(lonCodes.contains("LGW"), "Must contain LGW");
        assertTrue(lonCodes.contains("STN"), "Must contain STN");
    }

    @Test
    @DisplayName("Structured Normalized Model: verifies name, iataCode, cityName, countryName, coordinates")
    void testNormalizedModelFields() {
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
    @DisplayName("Unknown Location: returns empty list for nonsensical queries")
    void testUnknownLocation() {
        List<AirportResultDto> results = airportSearchService.searchAirports("NonexistentCity998877");
        assertTrue(results.isEmpty(), "Unknown non-IATA location should return empty list");
    }

    @Test
    @DisplayName("Edge Case: null or blank query returns empty list without error")
    void testNullOrBlankQuery() {
        assertTrue(airportSearchService.searchAirports(null).isEmpty());
        assertTrue(airportSearchService.searchAirports("").isEmpty());
        assertTrue(airportSearchService.searchAirports("   ").isEmpty());
    }
}
