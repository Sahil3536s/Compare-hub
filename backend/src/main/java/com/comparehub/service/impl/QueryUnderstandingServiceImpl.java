package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.SearchIntent;
import com.comparehub.service.EntityExtractor;
import com.comparehub.service.IntentClassifier;
import com.comparehub.service.QueryUnderstandingService;
import com.comparehub.service.QueryValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryUnderstandingServiceImpl implements QueryUnderstandingService {

    private final IntentClassifier intentClassifier;
    private final EntityExtractor entityExtractor;
    private final QueryValidationService queryValidationService;

    @Override
    public StructuredQueryUnderstandingDto understandQuery(String naturalLanguageQuery) {
        if (naturalLanguageQuery == null || naturalLanguageQuery.isBlank()) {
            return StructuredQueryUnderstandingDto.builder()
                    .intent(SearchIntent.UNKNOWN)
                    .confidence(0.0)
                    .originalQuery("")
                    .cleanedQuery("")
                    .isValid(false)
                    .clarificationPrompt("Please enter a query to search.")
                    .build();
        }

        String raw = naturalLanguageQuery.trim();

        // 1. Classify Intent
        SearchIntent intent = intentClassifier.classifyIntent(raw);
        double confidence = intentClassifier.calculateConfidence(raw, intent);

        // 2. Extract Entities per Intent
        ProductQueryEntitiesDto productEntities = null;
        FlightQueryEntitiesDto flightEntities = null;
        RideQueryEntitiesDto rideEntities = null;
        String cleaned = entityExtractor.cleanQuery(raw);

        switch (intent) {
            case FLIGHT_SEARCH -> {
                flightEntities = entityExtractor.extractFlightEntities(raw);
            }
            case RIDE_SEARCH -> {
                rideEntities = entityExtractor.extractRideEntities(raw);
            }
            case PRODUCT_SEARCH, UNKNOWN -> {
                productEntities = entityExtractor.extractProductEntities(raw);
            }
        }

        StructuredQueryUnderstandingDto result = StructuredQueryUnderstandingDto.builder()
                .intent(intent)
                .confidence(confidence)
                .originalQuery(raw)
                .cleanedQuery(cleaned)
                .productEntities(productEntities)
                .flightEntities(flightEntities)
                .rideEntities(rideEntities)
                .build();

        // 3. Validate Query
        queryValidationService.validateQuery(result);

        log.info("NLU Query Understanding for '{}' -> Intent: {} (confidence: {}), Valid: {}",
                raw, intent, confidence, result.getIsValid());

        return result;
    }
}
