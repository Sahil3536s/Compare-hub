package com.comparehub.service.impl;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.dto.RideCompareRequestDto;
import com.comparehub.dto.RideComparisonResponseDto;
import com.comparehub.dto.RouteEstimateResponseDto;
import com.comparehub.provider.RideProvider;
import com.comparehub.service.LocationService;
import com.comparehub.service.RideComparisonService;
import com.comparehub.service.RideRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideComparisonServiceImpl implements RideComparisonService {

    private final List<RideProvider> rideProviders;
    private final RideRankingService rideRankingService;
    private final LocationService locationService;
    private final com.comparehub.service.ComparisonRecommendationService recommendationService;

    @Override
    @Cacheable(
            value = "provider-health",
            key = "'ride_' + #request.pickup.latitude + '_' + #request.pickup.longitude + '_' + #request.destination.latitude + '_' + #request.destination.longitude + '_' + #request.rideType + '_' + #request.sortBy",
            unless = "#result == null || #result.offers.isEmpty()"
    )
    public RideComparisonResponseDto compareRides(RideCompareRequestDto request) {
        log.info("Calculating ride fare comparison between {} and {}",
                request.getPickup().getAddress(), request.getDestination().getAddress());

        // 1. Calculate road distance & route metrics
        RouteEstimateResponseDto route = locationService.calculateRoute(request.getPickup(), request.getDestination());
        double distanceKm = route.getDistanceKm();
        int durationMinutes = route.getDurationMinutes();

        // 2. Query all ride providers
        List<NormalizedRideOfferDto> rawOffers = new ArrayList<>();
        for (RideProvider provider : rideProviders) {
            try {
                List<NormalizedRideOfferDto> providerOffers = provider.getFareEstimate(
                        request.getPickup(), request.getDestination());
                if (providerOffers != null) {
                    rawOffers.addAll(providerOffers);
                }
            } catch (Exception e) {
                log.error("Ride provider '{}' failed: {}. Continuing with other providers.",
                        provider.getProviderName(), e.getMessage());
            }
        }

        // 3. Filter by vehicle category if specified
        List<NormalizedRideOfferDto> filteredOffers = rawOffers.stream()
                .filter(offer -> {
                    if (request.getRideType() != null && !request.getRideType().isBlank() && !"all".equalsIgnoreCase(request.getRideType())) {
                        String filter = request.getRideType().trim().toLowerCase();
                        String cat = offer.getVehicleCategory().toLowerCase();
                        if ("cab".equals(filter) && !("cab".equals(cat) || "premier".equals(cat))) {
                            return false;
                        } else if (!"cab".equals(filter) && !cat.contains(filter)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 4. Rank and score rides
        List<NormalizedRideOfferDto> rankedOffers = rideRankingService.rankAndBadgeRides(
                filteredOffers, request.getSortBy());

        // 5. Summary metrics
        BigDecimal cheapestFare = rankedOffers.stream()
                .map(NormalizedRideOfferDto::getEstimatedPriceMin)
                .min(BigDecimal::compareTo)
                .orElse(null);

        Integer fastestEta = rankedOffers.stream()
                .mapToInt(NormalizedRideOfferDto::getEtaMinutes)
                .min()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);

        NormalizedRideOfferDto bestOffer = rankedOffers.stream()
                .filter(NormalizedRideOfferDto::getIsBest)
                .findFirst()
                .orElse(null);

        AiRecommendationDto aiRecommendation = null;
        try {
            aiRecommendation = recommendationService.recommendRides(rankedOffers);
        } catch (Exception e) {
            log.warn("Ride AI recommendation skipped: {}", e.getMessage());
        }

        return RideComparisonResponseDto.builder()
                .pickup(request.getPickup())
                .destination(request.getDestination())
                .distanceKm(distanceKm)
                .durationMinutes(durationMinutes)
                .cheapestFare(cheapestFare)
                .fastestEtaMinutes(fastestEta)
                .bestProvider(bestOffer != null ? bestOffer.getProvider() + " (" + bestOffer.getRideType() + ")" : null)
                .offers(rankedOffers)
                .aiRecommendation(aiRecommendation)
                .rankingSummary(rideRankingService.getRankingSummary(rankedOffers))
                .build();
    }
}
