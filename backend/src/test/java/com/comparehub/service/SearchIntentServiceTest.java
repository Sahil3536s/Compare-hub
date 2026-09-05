package com.comparehub.service;

import com.comparehub.dto.SearchIntentResultDto;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.impl.SearchIntentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIntentServiceTest {

    private SearchIntentService searchIntentService;

    @BeforeEach
    void setUp() {
        searchIntentService = new SearchIntentServiceImpl();
    }

    @Test
    @DisplayName("Should detect PRODUCT_SEARCH and extract price & storage filters for Samsung query")
    void testDetectProductSearchWithFilters() {
        SearchIntentResultDto result = searchIntentService.detectIntent("Samsung phone under 30000 with 256GB");

        assertThat(result.getIntent()).isEqualTo(SearchIntent.PRODUCT_SEARCH);
        assertThat(result.getQuery()).isEqualTo("Samsung phone");
        assertThat(result.getConfidence()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.getFilters()).containsKey("maxPrice");
        assertThat((BigDecimal) result.getFilters().get("maxPrice")).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getFilters().get("storage")).isEqualTo("256GB");
        assertThat(result.getFilters().get("brand")).isEqualTo("Samsung");
    }

    @Test
    @DisplayName("Should detect PRODUCT_SEARCH for iPhone 17 256GB")
    void testDetectProductSearchIPhone() {
        SearchIntentResultDto result = searchIntentService.detectIntent("iPhone 17 256GB");

        assertThat(result.getIntent()).isEqualTo(SearchIntent.PRODUCT_SEARCH);
        assertThat(result.getFilters().get("storage")).isEqualTo("256GB");
        assertThat(result.getFilters().get("brand")).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should detect FLIGHT_SEARCH and extract origin, destination, and tomorrow date")
    void testDetectFlightSearchTomorrow() {
        SearchIntentResultDto result = searchIntentService.detectIntent("Delhi to Mumbai flight tomorrow");

        assertThat(result.getIntent()).isEqualTo(SearchIntent.FLIGHT_SEARCH);
        assertThat(result.getFlightParams().get("origin")).isEqualTo("DEL");
        assertThat(result.getFlightParams().get("destination")).isEqualTo("BOM");
        assertThat(result.getFlightParams().get("departureDate")).isEqualTo(LocalDate.now().plusDays(1).toString());
    }

    @Test
    @DisplayName("Should detect RIDE_SEARCH and extract pickup and destination from VIT Bhopal to Bhopal Airport")
    void testDetectRideSearch() {
        SearchIntentResultDto result = searchIntentService.detectIntent("ride from VIT Bhopal to Bhopal Airport");

        assertThat(result.getIntent()).isEqualTo(SearchIntent.RIDE_SEARCH);
        assertThat(result.getRideParams().get("pickup")).containsIgnoringCase("VIT Bhopal");
        assertThat(result.getRideParams().get("destination")).containsIgnoringCase("Bhopal Airport");
    }

    @Test
    @DisplayName("Should detect UNKNOWN intent for empty query")
    void testDetectUnknownIntentForBlank() {
        SearchIntentResultDto result = searchIntentService.detectIntent("   ");

        assertThat(result.getIntent()).isEqualTo(SearchIntent.UNKNOWN);
        assertThat(result.getConfidence()).isEqualTo(0.0);
    }
}
