package com.comparehub.service;

import com.comparehub.dto.CostTimeOptionDto;
import com.comparehub.dto.CostTimeOptimizationRequestDto;
import com.comparehub.dto.CostTimeOptimizationResponseDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.service.impl.CostTimeOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostTimeOptimizationServiceTest {

    private CostTimeOptimizationService service;

    @BeforeEach
    void setUp() {
        service = new CostTimeOptimizationServiceImpl();
    }

    private List<CostTimeOptionDto> sampleOptions() {
        return List.of(
                CostTimeOptionDto.builder()
                        .id("opt_a")
                        .title("Option A")
                        .cost(BigDecimal.valueOf(4200))
                        .durationMinutes(480) // 8h
                        .build(),
                CostTimeOptionDto.builder()
                        .id("opt_b")
                        .title("Option B")
                        .cost(BigDecimal.valueOf(5000))
                        .durationMinutes(300) // 5h
                        .build(),
                CostTimeOptionDto.builder()
                        .id("opt_c")
                        .title("Option C")
                        .cost(BigDecimal.valueOf(6100))
                        .durationMinutes(180) // 3h
                        .build()
        );
    }

    @Test
    @DisplayName("100% Money priority should rank Option A (Cheapest) as #1")
    void testRank100PercentMoney() {
        CostTimeOptimizationRequestDto request = CostTimeOptimizationRequestDto.builder()
                .costWeight(100.0)
                .timeWeight(0.0)
                .options(sampleOptions())
                .build();

        CostTimeOptimizationResponseDto response = service.optimize(request);

        assertNotNull(response);
        assertEquals("opt_a", response.getTopOption().getId());
        assertEquals("CHEAPEST", response.getTopOption().getClassification());
        assertEquals(100.0, response.getTopOption().getCostScore());
        assertEquals(0.0, response.getTopOption().getTimeScore());
    }

    @Test
    @DisplayName("100% Time priority should rank Option C (Fastest) as #1")
    void testRank100PercentTime() {
        CostTimeOptimizationRequestDto request = CostTimeOptimizationRequestDto.builder()
                .costWeight(0.0)
                .timeWeight(100.0)
                .options(sampleOptions())
                .build();

        CostTimeOptimizationResponseDto response = service.optimize(request);

        assertNotNull(response);
        assertEquals("opt_c", response.getTopOption().getId());
        assertEquals("FASTEST", response.getTopOption().getClassification());
        assertEquals(100.0, response.getTopOption().getTimeScore());
        assertEquals(0.0, response.getTopOption().getCostScore());
    }

    @Test
    @DisplayName("50/50 Balanced priority should evaluate optimal compromise")
    void testRankBalanced() {
        CostTimeOptimizationRequestDto request = CostTimeOptimizationRequestDto.builder()
                .costWeight(50.0)
                .timeWeight(50.0)
                .options(sampleOptions())
                .build();

        CostTimeOptimizationResponseDto response = service.optimize(request);

        assertNotNull(response);
        assertNotNull(response.getTopOption());
        assertEquals("BALANCED", response.getTopOption().getClassification());
        assertNotNull(response.getTradeoffSummary());
    }

    @Test
    @DisplayName("optimizeFlights should re-rank flight offers by cost vs time weight")
    void testOptimizeFlights() {
        List<NormalizedFlightOfferDto> flights = List.of(
                NormalizedFlightOfferDto.builder()
                        .airline("Slow Cheap Air")
                        .flightNumber("SC-101")
                        .price(BigDecimal.valueOf(3000))
                        .durationMinutes(360)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .airline("Fast Jet")
                        .flightNumber("FJ-202")
                        .price(BigDecimal.valueOf(7000))
                        .durationMinutes(120)
                        .build()
        );

        List<NormalizedFlightOfferDto> fastestFirst = service.optimizeFlights(flights, 0.0);
        assertEquals("Fast Jet", fastestFirst.get(0).getAirline());
        assertTrue(fastestFirst.get(0).getIsBest());

        List<NormalizedFlightOfferDto> cheapestFirst = service.optimizeFlights(flights, 100.0);
        assertEquals("Slow Cheap Air", cheapestFirst.get(0).getAirline());
        assertTrue(cheapestFirst.get(0).getIsBest());
    }
}
