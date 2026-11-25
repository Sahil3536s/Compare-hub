package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalSearchServiceImpl implements UniversalSearchService {

    private final SearchIntentService searchIntentService;
    private final QueryUnderstandingService queryUnderstandingService;
    private final ProductComparisonService productComparisonService;
    private final FlightComparisonService flightComparisonService;
    private final RideComparisonService rideComparisonService;
    private final LocationService locationService;
    private final SearchHistoryService searchHistoryService;

    @Override
    public UniversalSearchResponseDto executeUniversalSearch(UniversalSearchRequestDto request) {
        long startTime = System.currentTimeMillis();
        String rawQuery = request.getQuery() != null ? request.getQuery().trim() : "";

        // 1. Advanced Natural-Language Understanding
        StructuredQueryUnderstandingDto nlu = queryUnderstandingService.understandQuery(rawQuery);
        SearchIntent intent = nlu.getIntent();
        SearchIntentResultDto legacyDetails = searchIntentService.detectIntent(rawQuery);

        log.info("Universal Search query: '{}' -> NLU Intent: {} (confidence: {}), Valid: {}",
                rawQuery, intent, nlu.getConfidence(), nlu.getIsValid());

        UniversalSearchResponseDto.UniversalSearchResponseDtoBuilder responseBuilder = UniversalSearchResponseDto.builder()
                .intent(intent)
                .query(rawQuery)
                .intentDetails(legacyDetails)
                .queryUnderstanding(nlu);

        // 2. Check if mandatory fields are missing (e.g. flight origin)
        if (Boolean.FALSE.equals(nlu.getIsValid())) {
            long executionTime = System.currentTimeMillis() - startTime;
            return responseBuilder
                    .requiresClarification(true)
                    .missingFields(nlu.getMissingFields())
                    .clarificationPrompt(nlu.getClarificationPrompt())
                    .executionTimeMs(executionTime)
                    .build();
        }

        // 3. Route to existing comparison service with structured entities
        switch (intent) {
            case FLIGHT_SEARCH -> handleFlightSearch(nlu, legacyDetails, responseBuilder, request.getUserId());
            case RIDE_SEARCH -> handleRideSearch(nlu, legacyDetails, responseBuilder, request.getUserId());
            case PRODUCT_SEARCH, UNKNOWN -> handleProductSearch(nlu, legacyDetails, responseBuilder, request.getUserId());
        }

        long executionTime = System.currentTimeMillis() - startTime;
        responseBuilder.executionTimeMs(executionTime);

        return responseBuilder.build();
    }

    private void handleProductSearch(StructuredQueryUnderstandingDto nlu,
                                    SearchIntentResultDto intentDetails,
                                    UniversalSearchResponseDto.UniversalSearchResponseDtoBuilder responseBuilder,
                                    Long userId) {
        ProductQueryEntitiesDto prod = nlu.getProductEntities();

        String query = nlu.getCleanedQuery();
        if (query == null || query.isBlank()) {
            query = intentDetails.getQuery() != null ? intentDetails.getQuery() : intentDetails.getOriginalQuery();
        }

        BigDecimal maxPrice = prod != null ? prod.getMaxPrice() : null;
        BigDecimal minPrice = prod != null ? prod.getMinPrice() : null;
        String brand = prod != null && prod.getBrand() != null ? prod.getBrand() : "all";
        String category = prod != null && prod.getCategory() != null && !"general".equalsIgnoreCase(prod.getCategory())
                ? prod.getCategory()
                : "All Categories";
        String sortBy = prod != null && prod.getSortPreference() != null ? prod.getSortPreference() : "price_asc";

        // Query existing product comparison service
        ProductComparisonResponseDto productComparison = productComparisonService.compareProducts(
                query, "all", brand, category, minPrice, maxPrice, null, sortBy);

        responseBuilder
                .redirectRoute("/shopping")
                .productResults(productComparison)
                .aiRecommendation(productComparison.getAiRecommendation())
                .rankingSummary(productComparison.getRankingSummary());

        // Record Search History if authenticated
        if (userId != null) {
            try {
                searchHistoryService.recordSearch(SearchHistoryRequestDto.builder()
                        .userId(userId)
                        .query(query)
                        .searchType(com.comparehub.model.SearchType.SHOPPING)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to record product search history: {}", e.getMessage());
            }
        }
    }

    private void handleFlightSearch(StructuredQueryUnderstandingDto nlu,
                                   SearchIntentResultDto intentDetails,
                                   UniversalSearchResponseDto.UniversalSearchResponseDtoBuilder responseBuilder,
                                   Long userId) {
        FlightQueryEntitiesDto flight = nlu.getFlightEntities();

        String origin = flight != null && flight.getOrigin() != null && !flight.getOrigin().isBlank()
                ? flight.getOrigin() : intentDetails.getFlightParams().get("origin");
        String destination = flight != null && flight.getDestination() != null && !flight.getDestination().isBlank()
                ? flight.getDestination() : intentDetails.getFlightParams().get("destination");
        String departureDate = flight != null && flight.getDepartureDate() != null && !flight.getDepartureDate().isBlank()
                ? flight.getDepartureDate() : intentDetails.getFlightParams().get("departureDate");
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (origin == null || origin.isBlank()) missing.add("origin");
        if (destination == null || destination.isBlank()) missing.add("destination");
        if (departureDate == null || departureDate.isBlank()) missing.add("departureDate");
        if (!missing.isEmpty()) {
            responseBuilder.requiresClarification(true).missingFields(missing)
                    .clarificationPrompt("Please provide " + String.join(", ", missing) + " to compare flights.");
            return;
        }
        String sortBy = flight != null && flight.getSortPreference() != null ? flight.getSortPreference().toLowerCase() : "best";

        FlightSearchRequestDto flightRequest = FlightSearchRequestDto.builder()
                .userId(userId)
                .origin(origin)
                .destination(destination)
                .departureDate(departureDate)
                .adults(1)
                .cabinClass("ECONOMY")
                .sortBy(sortBy)
                .build();

        FlightComparisonResponseDto flightComparison = flightComparisonService.compareFlights(flightRequest);

        responseBuilder
                .redirectRoute("/flights")
                .flightResults(flightComparison)
                .aiRecommendation(flightComparison.getAiRecommendation())
                .rankingSummary(flightComparison.getRankingSummary());
    }

    private void handleRideSearch(StructuredQueryUnderstandingDto nlu,
                                 SearchIntentResultDto intentDetails,
                                 UniversalSearchResponseDto.UniversalSearchResponseDtoBuilder responseBuilder,
                                 Long userId) {
        RideQueryEntitiesDto ride = nlu.getRideEntities();

        String pickupStr = ride != null && ride.getPickup() != null && !ride.getPickup().isBlank()
                ? ride.getPickup() : intentDetails.getRideParams().get("pickup");
        String destinationStr = ride != null && ride.getDestination() != null && !ride.getDestination().isBlank()
                ? ride.getDestination() : intentDetails.getRideParams().get("destination");
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (pickupStr == null || pickupStr.isBlank()) missing.add("pickup");
        if (destinationStr == null || destinationStr.isBlank()) missing.add("destination");
        if (!missing.isEmpty()) {
            responseBuilder.requiresClarification(true).missingFields(missing)
                    .clarificationPrompt("Please provide " + String.join(" and ", missing) + " locations to compare rides.");
            return;
        }
        String rideType = ride != null && ride.getRideType() != null ? ride.getRideType() : "all";
        String sortBy = ride != null && ride.getSortPreference() != null ? ride.getSortPreference().toLowerCase() : "best";

        LocationDto pickupLoc;
        LocationDto destLoc;

        try {
            pickupLoc = locationService.geocodeAddress(pickupStr);
            destLoc = locationService.geocodeAddress(destinationStr);
            if (pickupLoc == null || destLoc == null
                    || pickupLoc.getLatitude() == null || pickupLoc.getLongitude() == null
                    || destLoc.getLatitude() == null || destLoc.getLongitude() == null) {
                throw new IllegalStateException("Geocoding did not return coordinates for both locations");
            }
        } catch (Exception e) {
            log.warn("Ride search requires valid geocoded locations: {}", e.getMessage());
            responseBuilder.requiresClarification(true)
                    .missingFields(java.util.List.of("valid pickup and destination locations"))
                    .clarificationPrompt("I couldn't verify both locations. Please check the pickup and destination names.");
            return;
        }

        RideCompareRequestDto rideRequest = RideCompareRequestDto.builder()
                .pickup(pickupLoc)
                .destination(destLoc)
                .rideType(rideType)
                .sortBy(sortBy)
                .build();

        RideComparisonResponseDto rideComparison = rideComparisonService.compareRides(rideRequest);

        responseBuilder
                .redirectRoute("/rides")
                .rideResults(rideComparison)
                .aiRecommendation(rideComparison.getAiRecommendation())
                .rankingSummary(rideComparison.getRankingSummary());
    }
}
