package com.comparehub.controller;

import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.service.FlightComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightComparisonController {

    private final FlightComparisonService flightComparisonService;

    @PostMapping("/search")
    public ResponseEntity<FlightComparisonResponseDto> searchFlights(
            @Valid @RequestBody FlightSearchRequestDto request) {

        FlightComparisonResponseDto response = flightComparisonService.compareFlights(request);
        return ResponseEntity.ok(response);
    }
}
