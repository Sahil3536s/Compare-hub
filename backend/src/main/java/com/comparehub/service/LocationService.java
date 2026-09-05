package com.comparehub.service;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;

import java.util.List;

public interface LocationService {

    List<PlaceSuggestionDto> suggestPlaces(String query);

    LocationDto geocodeAddress(String address);

    LocationDto reverseGeocode(Double latitude, Double longitude);

    RouteEstimateResponseDto calculateRoute(LocationDto pickup, LocationDto destination);
}
