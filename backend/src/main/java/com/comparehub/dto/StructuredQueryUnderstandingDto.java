package com.comparehub.dto;

import com.comparehub.model.SearchIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredQueryUnderstandingDto {

    private SearchIntent intent; // PRODUCT_SEARCH, FLIGHT_SEARCH, RIDE_SEARCH, UNKNOWN
    private double confidence;
    private String originalQuery;
    private String cleanedQuery;

    private ProductQueryEntitiesDto productEntities;
    private FlightQueryEntitiesDto flightEntities;
    private RideQueryEntitiesDto rideEntities;

    @Builder.Default
    private Boolean isValid = true;

    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    private String clarificationPrompt;
}
