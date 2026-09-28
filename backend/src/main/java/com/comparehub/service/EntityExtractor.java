package com.comparehub.service;

import com.comparehub.dto.FlightQueryEntitiesDto;
import com.comparehub.dto.ProductQueryEntitiesDto;
import com.comparehub.dto.RideQueryEntitiesDto;

public interface EntityExtractor {

    ProductQueryEntitiesDto extractProductEntities(String query);

    FlightQueryEntitiesDto extractFlightEntities(String query);

    RideQueryEntitiesDto extractRideEntities(String query);

    String cleanQuery(String query);
}
