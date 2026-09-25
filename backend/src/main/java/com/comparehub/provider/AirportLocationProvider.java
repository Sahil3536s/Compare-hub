package com.comparehub.provider;

import com.comparehub.dto.AirportResultDto;

import java.util.List;

/**
 * Contract for external airport and city lookup providers (e.g. Amadeus Reference Data Locations).
 */
public interface AirportLocationProvider {

    /**
     * Provider identification name.
     */
    String getProviderName();

    /**
     * Search airports and cities matching the given query keyword.
     *
     * @param query Search keyword (city name, airport name, or IATA code)
     * @param limit Maximum number of results to return
     * @return List of normalized AirportResultDto objects
     */
    List<AirportResultDto> searchAirports(String query, int limit);

    /**
     * Indicates whether this provider is enabled and configured with necessary credentials.
     */
    boolean isAvailable();
}
