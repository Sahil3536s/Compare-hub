package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.service.impl.GroupTravelRankingServiceImpl;
import com.comparehub.service.impl.GroupTravelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupTravelServiceTest {

    @Mock
    private GroupTransportModeEvaluator flightEvaluator;

    @Mock
    private GroupTransportModeEvaluator rideEvaluator;

    private GroupTravelRankingService rankingService;
    private GroupTravelService groupTravelService;

    @BeforeEach
    void setUp() {
        rankingService = new GroupTravelRankingServiceImpl();
        groupTravelService = new GroupTravelServiceImpl(
                List.of(flightEvaluator, rideEvaluator),
                rankingService
        );
    }

    @Test
    @DisplayName("Should compare group travel for 4 people and discover cab economies of scale")
    void testOptimizeGroupTravelForFourPeople() {
        GroupTravelOptionDto flightOpt = GroupTravelOptionDto.builder()
                .id("opt_flight")
                .mode(GroupTransportMode.FLIGHT_AND_RIDE)
                .title("Flight + Airport Cabs (IndiGo)")
                .totalCost(BigDecimal.valueOf(18600))
                .costPerPerson(BigDecimal.valueOf(4650))
                .estimatedTravelTimeMinutes(180)
                .formattedDuration("3h 00m")
                .numberOfTravelers(4)
                .requiredConnections(2)
                .build();

        GroupTravelOptionDto cabOpt = GroupTravelOptionDto.builder()
                .id("opt_cab")
                .mode(GroupTransportMode.DIRECT_RIDE)
                .title("Direct Cab (Uber XL)")
                .totalCost(BigDecimal.valueOf(6400))
                .costPerPerson(BigDecimal.valueOf(1600))
                .estimatedTravelTimeMinutes(300)
                .formattedDuration("5h 00m")
                .numberOfTravelers(4)
                .requiredConnections(0)
                .build();

        when(flightEvaluator.evaluate(any(GroupTravelRequestDto.class))).thenReturn(List.of(flightOpt));
        when(rideEvaluator.evaluate(any(GroupTravelRequestDto.class))).thenReturn(List.of(cabOpt));

        GroupTravelRequestDto request = GroupTravelRequestDto.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .numberOfTravelers(4)
                .priority("CHEAPEST")
                .build();

        GroupTravelResponseDto response = groupTravelService.optimizeGroupTravel(request);

        assertNotNull(response);
        assertEquals(4, response.getNumberOfTravelers());
        assertEquals(2, response.getOptions().size());
        assertNotNull(response.getBestValueOption());
        assertEquals("opt_cab", response.getBestValueOption().getId());
        assertEquals(BigDecimal.valueOf(6400), response.getCheapestOption().getTotalCost());
        assertEquals(BigDecimal.valueOf(1600), response.getCheapestOption().getCostPerPerson());
        assertNotNull(response.getGroupInsights());
        assertTrue(response.getGroupInsights().contains("saves"));
    }
}
