package com.comparehub.provider;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;

import java.util.List;

public interface LocationProvider {

    String getProviderName();

    List<PlaceSuggestionDto> getPlaceSuggestions(String query);

    LocationDto geocode(String address);

    LocationDto reverseGeocode(Double latitude, Double longitude);

    RouteEstimateResponseDto calculateRoute(LocationDto pickup, LocationDto destination);
}
