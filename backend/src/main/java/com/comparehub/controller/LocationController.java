package com.comparehub.controller;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/suggest")
    public ResponseEntity<List<PlaceSuggestionDto>> suggestPlaces(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        List<PlaceSuggestionDto> suggestions = locationService.suggestPlaces(query);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/geocode")
    public ResponseEntity<LocationDto> geocode(
            @RequestParam(name = "address") String address) {
        LocationDto location = locationService.geocodeAddress(address);
        return ResponseEntity.ok(location);
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<LocationDto> reverseGeocode(
            @RequestParam(name = "lat") Double latitude,
            @RequestParam(name = "lon") Double longitude) {
        LocationDto location = locationService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(location);
    }

    @PostMapping("/route-estimate")
    public ResponseEntity<RouteEstimateResponseDto> estimateRoute(
            @RequestBody LocationDto[] endpoints) {
        LocationDto pickup = endpoints.length > 0 ? endpoints[0] : null;
        LocationDto destination = endpoints.length > 1 ? endpoints[1] : null;

        if (pickup == null) pickup = LocationDto.builder().latitude(28.6315).longitude(77.2167).address("Connaught Place").build();
        if (destination == null) destination = LocationDto.builder().latitude(28.5562).longitude(77.1000).address("IGI Airport").build();

        RouteEstimateResponseDto estimate = locationService.calculateRoute(pickup, destination);
        return ResponseEntity.ok(estimate);
    }
}
