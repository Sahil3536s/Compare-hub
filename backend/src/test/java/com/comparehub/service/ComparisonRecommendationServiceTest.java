package com.comparehub.service;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.service.impl.ComparisonRecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonRecommendationServiceTest {

    private ComparisonRecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new ComparisonRecommendationServiceImpl();
    }

    @Test
    void shouldRecommendBestProductConsideringPriceRatingAndDelivery() {
        NormalizedProductOfferDto offer1 = NormalizedProductOfferDto.builder()
                .merchant("Amazon")
                .price(new BigDecimal("70499.00"))
                .rating(4.8)
                .delivery("Tomorrow")
                .discountPercent(12)
                .inStock(true)
                .build();

        NormalizedProductOfferDto offer2 = NormalizedProductOfferDto.builder()
                .merchant("Flipkart")
                .price(new BigDecimal("69999.00"))
                .rating(4.1)
                .delivery("In 4 days")
                .discountPercent(10)
                .inStock(true)
                .build();

        AiRecommendationDto result = recommendationService.recommendProducts(List.of(offer1, offer2));

        assertNotNull(result);
        assertEquals("Flipkart", result.getCheapest());
        assertEquals("Amazon", result.getBestOverall());
        assertTrue(result.getRecommendation().contains("Amazon costs ₹500.00 more than Flipkart"));
        assertTrue(result.getRecommendation().contains("higher rating"));
        assertFalse(result.getReasoningPoints().isEmpty());
    }

    @Test
    void shouldRecommendFastestAndCheapestFlightAccurately() {
        NormalizedFlightOfferDto flight1 = NormalizedFlightOfferDto.builder()
                .airline("IndiGo")
                .flightNumber("6E-204")
                .price(new BigDecimal("4999.00"))
                .durationMinutes(130)
                .stops(0)
                .isBest(true)
                .build();

        NormalizedFlightOfferDto flight2 = NormalizedFlightOfferDto.builder()
                .airline("SpiceJet")
                .flightNumber("SG-812")
                .price(new BigDecimal("4650.00"))
                .durationMinutes(220)
                .stops(1)
                .isBest(false)
                .build();

        AiRecommendationDto result = recommendationService.recommendFlights(List.of(flight1, flight2));

        assertNotNull(result);
        assertTrue(result.getCheapest().contains("SpiceJet"));
        assertTrue(result.getBestOverall().contains("IndiGo"));
        assertTrue(result.getRecommendation().contains("saves 1h 30m in travel time"));
        assertFalse(result.getReasoningPoints().isEmpty());
    }

    @Test
    void shouldRecommendRideConsideringFareAndEta() {
        NormalizedRideOfferDto ride1 = NormalizedRideOfferDto.builder()
                .provider("Uber")
                .rideType("Go")
                .estimatedPriceMin(new BigDecimal("280.00"))
                .etaMinutes(3)
                .vehicleCategory("Cab")
                .isBest(true)
                .build();

        NormalizedRideOfferDto ride2 = NormalizedRideOfferDto.builder()
                .provider("Rapido")
                .rideType("Bike")
                .estimatedPriceMin(new BigDecimal("120.00"))
                .etaMinutes(9)
                .vehicleCategory("Bike")
                .isBest(false)
                .build();

        AiRecommendationDto result = recommendationService.recommendRides(List.of(ride1, ride2));

        assertNotNull(result);
        assertTrue(result.getCheapest().contains("Rapido Bike"));
        assertTrue(result.getBestOverall().contains("Uber Go"));
        assertTrue(result.getRecommendation().contains("Uber Go arrives in 3 mins for ₹280.00"));
        assertFalse(result.getReasoningPoints().isEmpty());
    }
}
