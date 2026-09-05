package com.comparehub.controller;

import com.comparehub.dto.RideCompareRequestDto;
import com.comparehub.dto.RideComparisonResponseDto;
import com.comparehub.service.RideComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideComparisonController {

    private final RideComparisonService rideComparisonService;

    @PostMapping("/compare")
    public ResponseEntity<RideComparisonResponseDto> compareRides(
            @Valid @RequestBody RideCompareRequestDto request) {
        RideComparisonResponseDto response = rideComparisonService.compareRides(request);
        return ResponseEntity.ok(response);
    }
}
