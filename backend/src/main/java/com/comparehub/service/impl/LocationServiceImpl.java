package com.comparehub.service.impl;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.provider.LocationProvider;
import com.comparehub.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationProvider locationProvider;

    @Override
    public List<PlaceSuggestionDto> suggestPlaces(String query) {
        return locationProvider.getPlaceSuggestions(query);
    }

    @Override
    @Cacheable(value = "geocoding-locations", key = "'geo_' + #address.toLowerCase().trim()", unless = "#result == null")
    public LocationDto geocodeAddress(String address) {
        return locationProvider.geocode(address);
    }

    @Override
    @Cacheable(value = "geocoding-locations", key = "'revgeo_' + #latitude + '_' + #longitude", unless = "#result == null")
    public LocationDto reverseGeocode(Double latitude, Double longitude) {
        return locationProvider.reverseGeocode(latitude, longitude);
    }

    @Override
    public RouteEstimateResponseDto calculateRoute(LocationDto pickup, LocationDto destination) {
        return locationProvider.calculateRoute(pickup, destination);
    }
}
