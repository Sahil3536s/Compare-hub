package com.comparehub.service;

import com.comparehub.dto.GroupTransportMode;
import com.comparehub.dto.GroupTravelOptionDto;
import com.comparehub.service.impl.GroupTravelRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupTravelRankingServiceTest {

    private GroupTravelRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new GroupTravelRankingServiceImpl();
    }

    @Test
    @DisplayName("Should rank by CHEAPEST priority favoring lower total cost")
    void testRankByCheapest() {
        GroupTravelOptionDto expensiveFast = GroupTravelOptionDto.builder()
                .id("opt_flight")
                .mode(GroupTransportMode.FLIGHT_AND_RIDE)
                .totalCost(BigDecimal.valueOf(18600))
                .costPerPerson(BigDecimal.valueOf(4650))
                .estimatedTravelTimeMinutes(180)
                .requiredConnections(2)
                .build();

        GroupTravelOptionDto cheapSlow = GroupTravelOptionDto.builder()
                .id("opt_cab")
                .mode(GroupTransportMode.DIRECT_RIDE)
                .totalCost(BigDecimal.valueOf(6400))
                .costPerPerson(BigDecimal.valueOf(1600))
                .estimatedTravelTimeMinutes(300)
                .requiredConnections(0)
                .build();

        List<GroupTravelOptionDto> ranked = rankingService.rankOptions(List.of(expensiveFast, cheapSlow), "CHEAPEST", 4);

        assertNotNull(ranked);
        assertEquals(2, ranked.size());
        assertEquals("opt_cab", ranked.get(0).getId());
        assertTrue(ranked.get(0).isRecommended());
        assertEquals("BEST_GROUP_VALUE", ranked.get(0).getClassification());
    }

    @Test
    @DisplayName("Should rank by FASTEST priority favoring lower travel time")
    void testRankByFastest() {
        GroupTravelOptionDto expensiveFast = GroupTravelOptionDto.builder()
                .id("opt_flight")
                .mode(GroupTransportMode.FLIGHT_AND_RIDE)
                .totalCost(BigDecimal.valueOf(18600))
                .costPerPerson(BigDecimal.valueOf(4650))
                .estimatedTravelTimeMinutes(180)
                .formattedDuration("3h 00m")
                .requiredConnections(2)
                .build();

        GroupTravelOptionDto cheapSlow = GroupTravelOptionDto.builder()
                .id("opt_cab")
                .mode(GroupTransportMode.DIRECT_RIDE)
                .totalCost(BigDecimal.valueOf(6400))
                .costPerPerson(BigDecimal.valueOf(1600))
                .estimatedTravelTimeMinutes(300)
                .formattedDuration("5h 00m")
                .requiredConnections(0)
                .build();

        List<GroupTravelOptionDto> ranked = rankingService.rankOptions(List.of(expensiveFast, cheapSlow), "FASTEST", 4);

        assertNotNull(ranked);
        assertEquals("opt_flight", ranked.get(0).getId());
        assertTrue(ranked.get(0).isRecommended());
        assertEquals("FASTEST_OPTION", ranked.get(0).getClassification());
    }
}
