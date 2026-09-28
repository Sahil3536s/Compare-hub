package com.comparehub.service;

import com.comparehub.dto.AirportResultDto;

import java.util.List;

public interface AirportSearchService {

    /**
     * Search airports and multi-airport cities by query string (city, airport name, or IATA code).
     *
     * @param query search query
     * @return ranked list of matching airports and cities
     */
    List<AirportResultDto> searchAirports(String query);

    /**
     * Lookup an airport directly by its 3-letter IATA code.
     *
     * @param iataCode 3-letter IATA code
     * @return airport result DTO, or null if not found
     */
    AirportResultDto getAirportByIataCode(String iataCode);
}
