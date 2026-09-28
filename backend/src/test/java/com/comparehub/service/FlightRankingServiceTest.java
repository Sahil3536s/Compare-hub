package com.comparehub.service;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.impl.FlightRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightRankingServiceTest {

    private FlightRankingService rankingService;

    @BeforeEach
    void setUp() {
        RankingConfigProperties properties = new RankingConfigProperties();
        rankingService = new FlightRankingServiceImpl(properties);
    }

    @Test
    void shouldCorrectlyAssignCheapestFastestAndBestBadges() {
        // Fast & non-stop, but higher price
        NormalizedFlightOfferDto f1 = NormalizedFlightOfferDto.builder()
                .airline("Air India")
                .flightNumber("AI-101")
                .price(new BigDecimal("7000.00"))
                .durationMinutes(120)
                .stops(0)
                .build();

        // Slow 1-stop, but lowest price
        NormalizedFlightOfferDto f2 = NormalizedFlightOfferDto.builder()
                .airline("SpiceJet")
                .flightNumber("SG-202")
                .price(new BigDecimal("4000.00"))
                .durationMinutes(240)
                .stops(1)
                .build();

        // Balanced: Great price, fast, non-stop (Ideal Best Value)
        NormalizedFlightOfferDto f3 = NormalizedFlightOfferDto.builder()
                .airline("IndiGo")
                .flightNumber("6E-303")
                .price(new BigDecimal("4600.00"))
                .durationMinutes(125)
                .stops(0)
                .build();

        List<NormalizedFlightOfferDto> ranked = rankingService.rankAndBadgeFlights(
                List.of(f1, f2, f3), "best");

        assertEquals(3, ranked.size());

        // f2 is Cheapest
        assertTrue(f2.getIsCheapest());
        assertFalse(f1.getIsCheapest());

        // f1 is Fastest
        assertTrue(f1.getIsFastest());
        assertFalse(f2.getIsFastest());

        // f3 is Best Value
        assertTrue(f3.getIsBest());
        assertEquals("6E-303", ranked.get(0).getFlightNumber());

        RankingSummaryDto summary = rankingService.getRankingSummary(ranked);
        assertNotNull(summary);
        assertTrue(summary.getCheapest().contains("SpiceJet"));
        assertTrue(summary.getBestValue().contains("IndiGo"));
        assertTrue(summary.getFastest().contains("Air India"));
        assertEquals(0.50, summary.getWeights().get("price"));
        assertEquals(0.30, summary.getWeights().get("duration"));
        assertEquals(0.20, summary.getWeights().get("stops"));
    }
}
