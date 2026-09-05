package com.comparehub.service;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.impl.RideRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RideRankingServiceTest {

    private RideRankingService rankingService;

    @BeforeEach
    void setUp() {
        RankingConfigProperties properties = new RankingConfigProperties();
        rankingService = new RideRankingServiceImpl(properties);
    }

    @Test
    void shouldAssignCheapestFastestAndBestBadges() {
        NormalizedRideOfferDto cabUber = NormalizedRideOfferDto.builder()
                .provider("Uber")
                .rideType("Uber Go")
                .vehicleCategory("Cab")
                .estimatedPriceMin(new BigDecimal("280.00"))
                .etaMinutes(2)
                .build();

        NormalizedRideOfferDto cabOla = NormalizedRideOfferDto.builder()
                .provider("Ola")
                .rideType("Ola Mini")
                .vehicleCategory("Cab")
                .estimatedPriceMin(new BigDecimal("260.00"))
                .etaMinutes(5)
                .build();

        NormalizedRideOfferDto autoRapido = NormalizedRideOfferDto.builder()
                .provider("Rapido")
                .rideType("Rapido Auto")
                .vehicleCategory("Auto")
                .estimatedPriceMin(new BigDecimal("150.00"))
                .etaMinutes(3)
                .build();

        List<NormalizedRideOfferDto> ranked = rankingService.rankAndBadgeRides(
                List.of(cabUber, cabOla, autoRapido), "best");

        assertEquals(3, ranked.size());

        // autoRapido is overall cheapest
        assertTrue(autoRapido.getIsCheapest());
        assertFalse(cabUber.getIsCheapest());

        // cabUber is fastest (2 mins ETA)
        assertTrue(cabUber.getIsFastest());
        assertFalse(cabOla.getIsFastest());

        RankingSummaryDto summary = rankingService.getRankingSummary(ranked);
        assertNotNull(summary);
        assertTrue(summary.getCheapest().contains("Rapido"));
        assertTrue(summary.getFastest().contains("Uber"));
        assertEquals(0.60, summary.getWeights().get("fare"));
        assertEquals(0.40, summary.getWeights().get("eta"));
    }
}
