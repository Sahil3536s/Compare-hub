package com.comparehub.controller;

import com.comparehub.dto.SmartJourneyRequestDto;
import com.comparehub.dto.SmartJourneyResponseDto;
import com.comparehub.service.JourneyOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/journey")
@RequiredArgsConstructor
public class SmartJourneyController {

    private final JourneyOptimizationService journeyOptimizationService;

    @PostMapping("/optimize")
    public ResponseEntity<SmartJourneyResponseDto> optimizeJourney(@RequestBody SmartJourneyRequestDto request) {
        log.info("REST request to optimize door-to-door smart journey from '{}' to '{}' ({} travelers)",
                request.getOrigin(), request.getDestination(), request.getTravelers());
        SmartJourneyResponseDto response = journeyOptimizationService.optimizeJourney(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sample")
    public ResponseEntity<SmartJourneyResponseDto> getSampleJourney(
            @RequestParam(required = false, defaultValue = "Saket, South Delhi") String origin,
            @RequestParam(required = false, defaultValue = "Calangute, North Goa") String destination,
            @RequestParam(required = false, defaultValue = "1") int travelers,
            @RequestParam(required = false, defaultValue = "90") int airportBufferMinutes) {
        log.info("REST request for sample smart journey: '{}' -> '{}'", origin, destination);
        SmartJourneyResponseDto response = journeyOptimizationService.getSampleJourney(origin, destination, travelers, airportBufferMinutes);
        return ResponseEntity.ok(response);
    }
}
