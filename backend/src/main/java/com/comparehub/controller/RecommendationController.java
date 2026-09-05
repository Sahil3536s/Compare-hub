package com.comparehub.controller;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.service.ComparisonRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ComparisonRecommendationService recommendationService;

    @PostMapping("/products")
    public ResponseEntity<AiRecommendationDto> recommendProducts(
            @RequestBody List<NormalizedProductOfferDto> offers) {
        AiRecommendationDto recommendation = recommendationService.recommendProducts(offers);
        return ResponseEntity.ok(recommendation);
    }

    @PostMapping("/flights")
    public ResponseEntity<AiRecommendationDto> recommendFlights(
            @RequestBody List<NormalizedFlightOfferDto> offers) {
        AiRecommendationDto recommendation = recommendationService.recommendFlights(offers);
        return ResponseEntity.ok(recommendation);
    }

    @PostMapping("/rides")
    public ResponseEntity<AiRecommendationDto> recommendRides(
            @RequestBody List<NormalizedRideOfferDto> offers) {
        AiRecommendationDto recommendation = recommendationService.recommendRides(offers);
        return ResponseEntity.ok(recommendation);
    }
}
