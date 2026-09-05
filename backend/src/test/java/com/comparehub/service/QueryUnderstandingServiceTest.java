package com.comparehub.service;

import com.comparehub.dto.StructuredQueryUnderstandingDto;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.impl.EntityExtractorImpl;
import com.comparehub.service.impl.IntentClassifierImpl;
import com.comparehub.service.impl.QueryUnderstandingServiceImpl;
import com.comparehub.service.impl.QueryValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QueryUnderstandingServiceTest {

    private QueryUnderstandingService queryUnderstandingService;

    @BeforeEach
    void setUp() {
        IntentClassifier intentClassifier = new IntentClassifierImpl();
        EntityExtractor entityExtractor = new EntityExtractorImpl();
        QueryValidationService queryValidationService = new QueryValidationServiceImpl();
        queryUnderstandingService = new QueryUnderstandingServiceImpl(
                intentClassifier, entityExtractor, queryValidationService);
    }

    @Test
    @DisplayName("Should extract product intent, category, max price, and camera priority")
    void testParseCameraPhoneQuery() {
        String query = "Best camera phone under 35000";
        StructuredQueryUnderstandingDto result = queryUnderstandingService.understandQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(SearchIntent.PRODUCT_SEARCH);
        assertThat(result.getProductEntities()).isNotNull();
        assertThat(result.getProductEntities().getCategory()).isEqualTo("smartphones");
        assertThat(result.getProductEntities().getMaxPrice()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(result.getProductEntities().getPriority()).isEqualTo("camera");
        assertThat(result.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("Should extract brand, price ceiling, and storage from product query")
    void testParseBrandAndStorageQuery() {
        String query = "Samsung phone under 30000 with 256GB";
        StructuredQueryUnderstandingDto result = queryUnderstandingService.understandQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(SearchIntent.PRODUCT_SEARCH);
        assertThat(result.getProductEntities().getBrand()).isEqualTo("Samsung");
        assertThat(result.getProductEntities().getMaxPrice()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getProductEntities().getStorage()).isEqualTo("256GB");
        assertThat(result.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("Should extract non-stop stops, date, evening time, and route from flight query")
    void testParseComplexFlightQuery() {
        String query = "Cheapest non-stop Delhi to Bangalore flight Friday evening";
        StructuredQueryUnderstandingDto result = queryUnderstandingService.understandQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(SearchIntent.FLIGHT_SEARCH);
        assertThat(result.getFlightEntities()).isNotNull();
        assertThat(result.getFlightEntities().getOrigin()).isEqualTo("DEL");
        assertThat(result.getFlightEntities().getDestination()).isEqualTo("BLR");
        assertThat(result.getFlightEntities().getStops()).isEqualTo(0); // non-stop
        assertThat(result.getFlightEntities().getTimePreference()).isEqualTo("EVENING");
        assertThat(result.getFlightEntities().getSortPreference()).isEqualTo("PRICE");
        assertThat(result.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("Should extract dropoff and sort from ride query")
    void testParseRideQuery() {
        String query = "Find me a cheap ride to Bhopal airport";
        StructuredQueryUnderstandingDto result = queryUnderstandingService.understandQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(SearchIntent.RIDE_SEARCH);
        assertThat(result.getRideEntities()).isNotNull();
        assertThat(result.getRideEntities().getDestination()).contains("Airport");
        assertThat(result.getRideEntities().getSortPreference()).isEqualTo("CHEAPEST");
        assertThat(result.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("Should request clarification when flight origin is missing")
    void testMissingFlightOriginClarification() {
        String query = "Flights to Goa";
        StructuredQueryUnderstandingDto result = queryUnderstandingService.understandQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo(SearchIntent.FLIGHT_SEARCH);
        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getMissingFields()).contains("origin");
        assertThat(result.getClarificationPrompt()).isNotEmpty();
    }
}
