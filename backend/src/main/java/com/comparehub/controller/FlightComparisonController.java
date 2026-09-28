package com.comparehub.controller;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.service.AirportSearchService;
import com.comparehub.service.FlightComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightComparisonController {

    private final FlightComparisonService flightComparisonService;
    private final AirportSearchService airportSearchService;

    @GetMapping("/locations")
    public ResponseEntity<List<AirportResultDto>> searchLocations(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "q", required = false) String q) {

        String searchTerm = (query != null && !query.isBlank()) ? query : (q != null ? q : "");
        List<AirportResultDto> results = airportSearchService.searchAirports(searchTerm);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/search")
    public ResponseEntity<FlightComparisonResponseDto> searchFlights(
            @Valid @RequestBody FlightSearchRequestDto request) {

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        return ResponseEntity.ok(response);
    }
}
