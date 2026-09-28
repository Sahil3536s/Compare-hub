package com.comparehub.service;

import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.provider.impl.AmadeusFlightProvider;
import com.comparehub.provider.impl.DomesticAirlinesFlightProvider;
import com.comparehub.service.impl.FlightComparisonServiceImpl;
import com.comparehub.service.impl.FlightRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightRouteCoverageTest {

    private FlightComparisonService flightComparisonService;

    @BeforeEach
    void setUp() {
        DomesticAirlinesFlightProvider domesticProvider = new DomesticAirlinesFlightProvider();
        AmadeusFlightProvider amadeusProvider = new AmadeusFlightProvider();
        com.comparehub.config.RankingConfigProperties properties = new com.comparehub.config.RankingConfigProperties();
        FlightRankingService rankingService = new FlightRankingServiceImpl(properties);
        ComparisonRecommendationService recommendationService = Mockito.mock(ComparisonRecommendationService.class);

        flightComparisonService = new FlightComparisonServiceImpl(
                List.of(domesticProvider, amadeusProvider),
                rankingService,
                recommendationService
        );
    }

    @Test
    @DisplayName("Route 1: Delhi (DEL) -> Mumbai (BOM)")
    void testRoute_DelhiToMumbai() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("DEL")
                .destination("BOM")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "DEL -> BOM must return flight offers");
        assertNotNull(response.getCheapestPrice());
        assertNotNull(response.getFastestDurationMinutes());
    }

    @Test
    @DisplayName("Route 2: Bhopal (BHO) -> Delhi (DEL)")
    void testRoute_BhopalToDelhi() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("BHO")
                .destination("DEL")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "BHO -> DEL must return flight offers");
        assertTrue(response.getOffers().stream().anyMatch(o -> o.getOrigin().equals("BHO")));
    }

    @Test
    @DisplayName("Route 3: Indore (IDR) -> Bangalore (BLR)")
    void testRoute_IndoreToBangalore() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("IDR")
                .destination("BLR")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "IDR -> BLR must return flight offers");
        assertTrue(response.getOffers().stream().anyMatch(o -> o.getDestination().equals("BLR")));
    }

    @Test
    @DisplayName("Route 4: Mumbai (BOM) -> Dubai (DXB) (International Corridor)")
    void testRoute_MumbaiToDubai() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("BOM")
                .destination("DXB")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "BOM -> DXB must return international flight offers");
        assertTrue(response.getOffers().stream().anyMatch(o -> o.getAirline().contains("Emirates") || o.getAirline().contains("Air India")));
    }

    @Test
    @DisplayName("Route 5: Delhi (DEL) -> London (LHR) (Long-haul International)")
    void testRoute_DelhiToLondon() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("DEL")
                .destination("LHR")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "DEL -> LHR must return international flight offers");
        assertTrue(response.getOffers().stream().anyMatch(o -> o.getAirline().contains("British Airways") || o.getAirline().contains("Air India")));
    }

    @Test
    @DisplayName("Route 6: New York (JFK) -> Los Angeles (LAX) (US Domestic Corridor)")
    void testRoute_NewYorkToLosAngeles() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("JFK")
                .destination("LAX")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertTrue(response.getTotalOffers() > 0, "JFK -> LAX must return flight offers");
    }

    @Test
    @DisplayName("Real Data vs Location Data: Valid airport with no flights (e.g. BHO -> DXB) returns 0 offers")
    void testRoute_ValidAirportWithNoFlights() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("BHO")
                .destination("DXB")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        assertNotNull(response);
        assertEquals(0, response.getTotalOffers(), "Connected providers do not have direct flights for BHO -> DXB");
        assertTrue(response.getOffers().isEmpty(), "Must not fabricate fake flights");
    }

    @Test
    @DisplayName("Validation: Same origin and destination throws IllegalArgumentException")
    void testRoute_SameAirportThrowsException() {
        FlightSearchRequestDto request = FlightSearchRequestDto.builder()
                .origin("DEL")
                .destination("DEL")
                .departureDate("2026-10-20")
                .adults(1)
                .build();

        assertThrows(IllegalArgumentException.class, () -> flightComparisonService.compareFlights(request));
    }
}
