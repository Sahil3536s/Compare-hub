package com.comparehub.service.impl;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.provider.FlightProvider;
import com.comparehub.service.FlightComparisonService;
import com.comparehub.service.FlightRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightComparisonServiceImpl implements FlightComparisonService {

    private final List<FlightProvider> flightProviders;
    private final FlightRankingService flightRankingService;
    private final com.comparehub.service.ComparisonRecommendationService recommendationService;

    @Override
    @Cacheable(
            value = "flight-searches",
            key = "#request.origin + '_' + #request.destination + '_' + #request.departureDate + '_' + #request.cabinClass + '_' + #request.maxStops + '_' + #request.airline + '_' + #request.maxPrice + '_' + #request.timeOfDay + '_' + #request.sortBy",
            unless = "#result == null || #result.offers.isEmpty()"
    )
    public FlightComparisonResponseDto compareFlights(FlightSearchRequestDto request) {
        log.info("Performing flight search comparison: {} -> {} on date: {}",
                request.getOrigin(), request.getDestination(), request.getDepartureDate());

        // 1. Gather offers from all registered flight providers
        List<NormalizedFlightOfferDto> rawOffers = new ArrayList<>();
        for (FlightProvider provider : flightProviders) {
            try {
                List<NormalizedFlightOfferDto> results = provider.searchFlights(request);
                if (results != null) {
                    rawOffers.addAll(results);
                }
            } catch (Exception e) {
                log.error("Flight provider '{}' failed during search: {}. Continuing with remaining providers.",
                        provider.getProviderName(), e.getMessage());
            }
        }

        // 2. Filter offers according to user constraints
        List<NormalizedFlightOfferDto> filteredOffers = rawOffers.stream()
                .filter(offer -> {
                    // Number of stops filter
                    if (request.getMaxStops() != null && offer.getStops() > request.getMaxStops()) {
                        return false;
                    }
                    // Airline filter
                    if (request.getAirline() != null && !request.getAirline().isBlank() && !"all".equalsIgnoreCase(request.getAirline())) {
                        if (!offer.getAirline().equalsIgnoreCase(request.getAirline().trim())) {
                            return false;
                        }
                    }
                    // Max price filter
                    if (request.getMaxPrice() != null && offer.getPrice().compareTo(request.getMaxPrice()) > 0) {
                        return false;
                    }
                    // Time of day filter
                    if (request.getTimeOfDay() != null && !request.getTimeOfDay().isBlank() && !"all".equalsIgnoreCase(request.getTimeOfDay())) {
                        if (!matchesTimeOfDay(offer.getDeparture(), request.getTimeOfDay())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 3. Rank, calculate scores, and assign badges (Cheapest, Fastest, Best)
        List<NormalizedFlightOfferDto> rankedOffers = flightRankingService.rankAndBadgeFlights(
                filteredOffers, request.getSortBy());

        // 4. Compute Summary Insights
        BigDecimal cheapestPrice = rankedOffers.stream()
                .map(NormalizedFlightOfferDto::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        Integer fastestDuration = rankedOffers.stream()
                .mapToInt(NormalizedFlightOfferDto::getDurationMinutes)
                .min()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);

        NormalizedFlightOfferDto bestOffer = rankedOffers.stream()
                .filter(NormalizedFlightOfferDto::getIsBest)
                .findFirst()
                .orElse(null);

        AiRecommendationDto aiRecommendation = null;
        try {
            aiRecommendation = recommendationService.recommendFlights(rankedOffers);
        } catch (Exception e) {
            log.warn("Flight AI recommendation skipped: {}", e.getMessage());
        }

        return FlightComparisonResponseDto.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureDate(request.getDepartureDate())
                .returnDate(request.getReturnDate())
                .totalOffers(rankedOffers.size())
                .cheapestPrice(cheapestPrice)
                .fastestDurationMinutes(fastestDuration)
                .bestAirline(bestOffer != null ? bestOffer.getAirline() : null)
                .offers(rankedOffers)
                .aiRecommendation(aiRecommendation)
                .rankingSummary(flightRankingService.getRankingSummary(rankedOffers))
                .build();
    }

    private boolean matchesTimeOfDay(String departureTime, String timeOfDay) {
        if (departureTime == null) return true;
        try {
            int hour = Integer.parseInt(departureTime.split(":")[0].trim());
            return switch (timeOfDay.toLowerCase().trim()) {
                case "morning" -> hour >= 5 && hour < 12;
                case "afternoon" -> hour >= 12 && hour < 18;
                case "evening" -> hour >= 18 && hour <= 23;
                case "night" -> hour >= 0 && hour < 5;
                default -> true;
            };
        } catch (Exception e) {
            return true;
        }
    }
}
