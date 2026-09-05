package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.service.impl.CostTimeOptimizationServiceImpl;
import com.comparehub.service.impl.JourneyOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JourneyOptimizationServiceTest {

    private JourneyOptimizationService journeyOptimizationService;
    private CostTimeOptimizationService costTimeOptimizationService;

    @BeforeEach
    void setUp() {
        costTimeOptimizationService = new CostTimeOptimizationServiceImpl();
        journeyOptimizationService = new JourneyOptimizationServiceImpl(costTimeOptimizationService);
    }

    @Test
    @DisplayName("Should generate valid door-to-door journey combinations with timing buffers and total costs")
    void testOptimizeJourneyDoorToDoor() {
        SmartJourneyRequestDto request = SmartJourneyRequestDto.builder()
                .origin("Saket, South Delhi")
                .destination("Candolim, North Goa")
                .travelers(1)
                .airportBufferMinutes(90)
                .costWeight(50.0)
                .timeWeight(50.0)
                .build();

        SmartJourneyResponseDto response = journeyOptimizationService.optimizeJourney(request);

        assertNotNull(response);
        assertNotNull(response.getAllCombinations());
        assertFalse(response.getAllCombinations().isEmpty());

        // Check top options identified
        assertNotNull(response.getCheapestJourney());
        assertNotNull(response.getFastestJourney());
        assertNotNull(response.getBalancedJourney());

        // Verify segments in the IndiGo option (opt-balanced-1)
        JourneyOptionDto indigoOption = response.getAllCombinations().stream()
                .filter(j -> "opt-balanced-1".equals(j.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(indigoOption);
        assertEquals(5, indigoOption.getSegments().size());

        // Ride 1 (₹350) + Buffer (₹0) + Flight (₹4850) + Buffer (₹0) + Ride 2 (₹620) = ₹5820
        assertEquals(BigDecimal.valueOf(5820.0), indigoOption.getTotalCost());
        assertTrue(indigoOption.getTotalDurationMinutes() > 200);

        // Verify Tradeoff Summary generated
        assertNotNull(response.getTradeoffSummary());
    }

    @Test
    @DisplayName("Should re-rank cheapest vs fastest when cost weight slider changes")
    void testCostWeightTradeoff() {
        SmartJourneyRequestDto cheapRequest = SmartJourneyRequestDto.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(1)
                .airportBufferMinutes(90)
                .costWeight(100.0) // 100% Money
                .timeWeight(0.0)
                .build();

        SmartJourneyResponseDto cheapResponse = journeyOptimizationService.optimizeJourney(cheapRequest);
        assertEquals(cheapResponse.getCheapestJourney().getId(), cheapResponse.getTopRecommendedJourney().getId());

        SmartJourneyRequestDto fastRequest = SmartJourneyRequestDto.builder()
                .origin("Delhi")
                .destination("Goa")
                .travelers(1)
                .airportBufferMinutes(90)
                .costWeight(0.0) // 100% Time
                .timeWeight(100.0)
                .build();

        SmartJourneyResponseDto fastResponse = journeyOptimizationService.optimizeJourney(fastRequest);
        assertEquals(fastResponse.getFastestJourney().getId(), fastResponse.getTopRecommendedJourney().getId());
    }
}
