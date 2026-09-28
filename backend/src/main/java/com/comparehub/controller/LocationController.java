package com.comparehub.controller;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.PlaceSuggestionDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.exception.BadRequestException;
import com.comparehub.exception.ResourceNotFoundException;
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
            @RequestParam(name = "address", required = false) String address) {
        if (address == null || address.trim().isBlank()) {
            throw new BadRequestException("Address parameter is required.");
        }
        LocationDto location = locationService.geocodeAddress(address.trim());
        if (location == null) {
            throw new ResourceNotFoundException("No matching location found.");
        }
        return ResponseEntity.ok(location);
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<LocationDto> reverseGeocode(
            @RequestParam(name = "lat", required = false) Double latitude,
            @RequestParam(name = "lon", required = false) Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("Latitude and longitude parameters are required.");
        }
        LocationDto location = locationService.reverseGeocode(latitude, longitude);
        if (location == null) {
            throw new ResourceNotFoundException("No matching location found.");
        }
        return ResponseEntity.ok(location);
    }

    @PostMapping("/route-estimate")
    public ResponseEntity<RouteEstimateResponseDto> estimateRoute(
            @RequestBody LocationDto[] endpoints) {
        if (endpoints == null || endpoints.length < 1 || endpoints[0] == null) {
            throw new BadRequestException("Pickup location is required.");
        }
        if (endpoints.length < 2 || endpoints[1] == null) {
            throw new BadRequestException("Destination location is required.");
        }

        LocationDto pickup = endpoints[0];
        LocationDto destination = endpoints[1];

        boolean pickupHasCoords = pickup.getLatitude() != null && pickup.getLongitude() != null;
        boolean pickupHasAddr = pickup.getAddress() != null && !pickup.getAddress().isBlank();
        if (!pickupHasCoords && !pickupHasAddr) {
            throw new BadRequestException("Pickup location is required.");
        }

        boolean destHasCoords = destination.getLatitude() != null && destination.getLongitude() != null;
        boolean destHasAddr = destination.getAddress() != null && !destination.getAddress().isBlank();
        if (!destHasCoords && !destHasAddr) {
            throw new BadRequestException("Destination location is required.");
        }

        if (!pickupHasCoords) {
            LocationDto geocoded = locationService.geocodeAddress(pickup.getAddress());
            if (geocoded == null || geocoded.getLatitude() == null || geocoded.getLongitude() == null) {
                throw new BadRequestException("Pickup location could not be geocoded: " + pickup.getAddress());
            }
            pickup = geocoded;
        }

        if (!destHasCoords) {
            LocationDto geocoded = locationService.geocodeAddress(destination.getAddress());
            if (geocoded == null || geocoded.getLatitude() == null || geocoded.getLongitude() == null) {
                throw new BadRequestException("Destination location could not be geocoded: " + destination.getAddress());
            }
            destination = geocoded;
        }

        RouteEstimateResponseDto estimate = locationService.calculateRoute(pickup, destination);
        return ResponseEntity.ok(estimate);
    }
}

