package com.comparehub.service.impl;

import com.comparehub.dto.FlightQueryEntitiesDto;
import com.comparehub.dto.ProductQueryEntitiesDto;
import com.comparehub.dto.RideQueryEntitiesDto;
import com.comparehub.dto.StructuredQueryUnderstandingDto;
import com.comparehub.service.QueryValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class QueryValidationServiceImpl implements QueryValidationService {

    @Override
    public void validateQuery(StructuredQueryUnderstandingDto dto) {
        if (dto == null || dto.getIntent() == null) return;

        List<String> missing = new ArrayList<>();
        String prompt = null;
        boolean valid = true;

        switch (dto.getIntent()) {
            case FLIGHT_SEARCH -> {
                FlightQueryEntitiesDto flight = dto.getFlightEntities();
                if (flight == null || flight.getOrigin() == null || flight.getOrigin().isBlank()) {
                    missing.add("origin");
                }
                if (flight == null || flight.getDestination() == null || flight.getDestination().isBlank()) {
                    missing.add("destination");
                }

                if (!missing.isEmpty()) {
                    valid = false;
                    if (missing.contains("origin") && missing.contains("destination")) {
                        prompt = "Please specify your departure and destination cities (e.g. Delhi to Bangalore).";
                    } else if (missing.contains("origin")) {
                        prompt = String.format("Where are you flying to %s from? (e.g. Delhi, Mumbai)", flight != null ? flight.getDestination() : "");
                    } else {
                        prompt = String.format("Where are you flying from %s to? (e.g. Bangalore, Goa)", flight != null ? flight.getOrigin() : "");
                    }
                }
            }
            case RIDE_SEARCH -> {
                RideQueryEntitiesDto ride = dto.getRideEntities();
                if (ride == null || ride.getDestination() == null || ride.getDestination().isBlank()) {
                    missing.add("destination");
                    valid = false;
                    prompt = "Please specify where you would like to go (e.g. Bhopal Airport, Connaught Place).";
                }
            }
            case PRODUCT_SEARCH -> {
                ProductQueryEntitiesDto prod = dto.getProductEntities();
                if ((prod == null || (prod.getCategory() == null && prod.getBrand() == null)) &&
                    (dto.getCleanedQuery() == null || dto.getCleanedQuery().isBlank())) {
                    missing.add("product");
                    valid = false;
                    prompt = "Please enter a product name, category, or brand to search.";
                }
            }
            default -> {
                valid = true;
            }
        }

        dto.setIsValid(valid);
        dto.setMissingFields(missing);
        dto.setClarificationPrompt(prompt);
    }
}
