package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversalSearchServiceTest {

    @Mock
    private SearchIntentService searchIntentService;

    @Mock
    private ProductComparisonService productComparisonService;

    @Mock
    private FlightComparisonService flightComparisonService;

    @Mock
    private RideComparisonService rideComparisonService;

    @Mock
    private LocationService locationService;

    @Mock
    private SearchHistoryService searchHistoryService;

    private UniversalSearchService universalSearchService;

    @BeforeEach
    void setUp() {
        IntentClassifier intentClassifier = new IntentClassifierImpl();
        EntityExtractor entityExtractor = new EntityExtractorImpl();
        QueryValidationService queryValidationService = new QueryValidationServiceImpl();
        QueryUnderstandingService queryUnderstandingService = new QueryUnderstandingServiceImpl(
                intentClassifier, entityExtractor, queryValidationService);

        universalSearchService = new UniversalSearchServiceImpl(
                searchIntentService,
                queryUnderstandingService,
                productComparisonService,
                flightComparisonService,
                rideComparisonService,
                locationService,
                searchHistoryService
        );
    }

    @Test
    @DisplayName("Should route PRODUCT_SEARCH to ProductComparisonService")
    void testRouteProductSearch() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("maxPrice", new BigDecimal("30000"));
        filters.put("brand", "Samsung");

        SearchIntentResultDto intentResult = SearchIntentResultDto.builder()
                .intent(SearchIntent.PRODUCT_SEARCH)
                .originalQuery("Samsung phone under 30000")
                .query("Samsung phone")
                .confidence(0.92)
                .filters(filters)
                .build();

        ProductComparisonResponseDto mockProductResponse = ProductComparisonResponseDto.builder()
                .query("Samsung phone")
                .totalOffers(5)
                .offers(Collections.emptyList())
                .build();

        when(searchIntentService.detectIntent(anyString())).thenReturn(intentResult);
        when(productComparisonService.compareProducts(anyString(), anyString(), anyString(), anyString(), isNull(), any(), isNull(), anyString()))
                .thenReturn(mockProductResponse);

        UniversalSearchResponseDto response = universalSearchService.executeUniversalSearch(
                UniversalSearchRequestDto.builder().query("Samsung phone under 30000").build());

        assertThat(response.getIntent()).isEqualTo(SearchIntent.PRODUCT_SEARCH);
        assertThat(response.getRedirectRoute()).isEqualTo("/shopping");
        assertThat(response.getProductResults()).isNotNull();
        assertThat(response.getProductResults().getTotalOffers()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should route FLIGHT_SEARCH to FlightComparisonService")
    void testRouteFlightSearch() {
        Map<String, String> flightParams = new HashMap<>();
        flightParams.put("origin", "DEL");
        flightParams.put("destination", "BOM");
        flightParams.put("departureDate", "2026-09-10");

        SearchIntentResultDto intentResult = SearchIntentResultDto.builder()
                .intent(SearchIntent.FLIGHT_SEARCH)
                .originalQuery("Delhi to Mumbai flight")
                .query("DEL to BOM Flights")
                .confidence(0.95)
                .flightParams(flightParams)
                .build();

        FlightComparisonResponseDto mockFlightResponse = FlightComparisonResponseDto.builder()
                .origin("DEL")
                .destination("BOM")
                .totalOffers(4)
                .offers(Collections.emptyList())
                .build();

        when(searchIntentService.detectIntent(anyString())).thenReturn(intentResult);
        when(flightComparisonService.compareFlights(any(FlightSearchRequestDto.class))).thenReturn(mockFlightResponse);

        UniversalSearchResponseDto response = universalSearchService.executeUniversalSearch(
                UniversalSearchRequestDto.builder().query("Delhi to Mumbai flight").build());

        assertThat(response.getIntent()).isEqualTo(SearchIntent.FLIGHT_SEARCH);
        assertThat(response.getRedirectRoute()).isEqualTo("/flights");
        assertThat(response.getFlightResults()).isNotNull();
        verify(flightComparisonService).compareFlights(any(FlightSearchRequestDto.class));
    }
}
