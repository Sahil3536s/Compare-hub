package com.comparehub.service.impl;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.FlightRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FlightRankingServiceImpl implements FlightRankingService {

    private final RankingConfigProperties rankingConfigProperties;

    @Override
    public List<NormalizedFlightOfferDto> rankAndBadgeFlights(
            List<NormalizedFlightOfferDto> offers, String sortBy) {

        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        RankingConfigProperties.FlightWeights weights = rankingConfigProperties.getFlight();
        double wPrice = weights.getPriceWeight();
        double wDuration = weights.getDurationWeight();
        double wStops = weights.getStopsWeight();

        // 1. Min/Max bounds for normalization
        BigDecimal minPrice = offers.stream().map(NormalizedFlightOfferDto::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.valueOf(1000));
        BigDecimal maxPrice = offers.stream().map(NormalizedFlightOfferDto::getPrice).max(BigDecimal::compareTo).orElse(minPrice);
        double priceRange = maxPrice.subtract(minPrice).doubleValue();

        int minDuration = offers.stream().mapToInt(NormalizedFlightOfferDto::getDurationMinutes).min().orElse(60);
        int maxDuration = offers.stream().mapToInt(NormalizedFlightOfferDto::getDurationMinutes).max().orElse(minDuration);
        double durationRange = maxDuration - minDuration;

        // 2. Normalize and compute deterministic Best Value score
        double highestScore = -1.0;
        NormalizedFlightOfferDto bestOfferCandidate = null;

        for (NormalizedFlightOfferDto offer : offers) {
            // Price: lower is better -> [0.0, 1.0]
            double normPrice = (priceRange > 0)
                    ? (maxPrice.subtract(offer.getPrice()).doubleValue()) / priceRange
                    : 1.0;

            // Duration: lower is better -> [0.0, 1.0]
            double normDuration = (durationRange > 0)
                    ? ((double) (maxDuration - offer.getDurationMinutes())) / durationRange
                    : 1.0;

            // Stops: 0 stops = 1.0, 1 stop = 0.5, >=2 stops = 0.0
            int stops = offer.getStops() != null ? offer.getStops() : 0;
            double normStops = stops == 0 ? 1.0 : (stops == 1 ? 0.5 : 0.0);

            // Deterministic score (0.0 to 1.0)
            double score = (normPrice * wPrice) + (normDuration * wDuration) + (normStops * wStops);
            BigDecimal roundedScore = BigDecimal.valueOf(score * 100.0).setScale(1, RoundingMode.HALF_UP);
            offer.setScore(roundedScore.doubleValue());

            if (score > highestScore) {
                highestScore = score;
                bestOfferCandidate = offer;
            }
        }

        // 3. Assign Badges
        for (NormalizedFlightOfferDto offer : offers) {
            offer.setIsCheapest(offer.getPrice().compareTo(minPrice) == 0);
            offer.setIsFastest(offer.getDurationMinutes() == minDuration);
            offer.setIsBest(bestOfferCandidate != null && offer.equals(bestOfferCandidate));
        }

        // 4. Apply Sorting
        List<NormalizedFlightOfferDto> sorted = new ArrayList<>(offers);
        String strategy = sortBy != null ? sortBy.toLowerCase().trim() : "best";

        switch (strategy) {
            case "cheapest":
            case "price_asc":
            case "price-asc":
                sorted.sort(Comparator.comparing(NormalizedFlightOfferDto::getPrice));
                break;
            case "price_desc":
            case "price-desc":
                sorted.sort(Comparator.comparing(NormalizedFlightOfferDto::getPrice).reversed());
                break;
            case "fastest":
            case "duration":
                sorted.sort(Comparator.comparing(NormalizedFlightOfferDto::getDurationMinutes));
                break;
            case "best":
            default:
                sorted.sort(Comparator.comparing(NormalizedFlightOfferDto::getScore).reversed());
                break;
        }

        return sorted;
    }

    @Override
    public RankingSummaryDto getRankingSummary(List<NormalizedFlightOfferDto> rankedOffers) {
        if (rankedOffers == null || rankedOffers.isEmpty()) {
            return RankingSummaryDto.builder()
                    .cheapest("N/A")
                    .bestValue("N/A")
                    .highestRated("N/A")
                    .fastest("N/A")
                    .explanation("No flights to rank.")
                    .build();
        }

        RankingConfigProperties.FlightWeights weights = rankingConfigProperties.getFlight();
        Map<String, Double> weightMap = Map.of(
                "price", weights.getPriceWeight(),
                "duration", weights.getDurationWeight(),
                "stops", weights.getStopsWeight()
        );

        NormalizedFlightOfferDto cheapest = rankedOffers.stream().filter(NormalizedFlightOfferDto::getIsCheapest).findFirst().orElse(rankedOffers.get(0));
        NormalizedFlightOfferDto best = rankedOffers.stream().filter(NormalizedFlightOfferDto::getIsBest).findFirst().orElse(rankedOffers.get(0));
        NormalizedFlightOfferDto fastest = rankedOffers.stream().filter(NormalizedFlightOfferDto::getIsFastest).findFirst().orElse(rankedOffers.get(0));

        String explanation = String.format(
                "Best flight score is calculated deterministically: Airfare (%d%%), Flight Duration (%d%%), Number of Stops (%d%%).",
                Math.round(weights.getPriceWeight() * 100),
                Math.round(weights.getDurationWeight() * 100),
                Math.round(weights.getStopsWeight() * 100)
        );

        return RankingSummaryDto.builder()
                .cheapest(cheapest.getAirline() + " (" + cheapest.getFlightNumber() + " - ₹" + cheapest.getPrice() + ")")
                .bestValue(best.getAirline() + " (" + best.getFlightNumber() + " - Score: " + best.getScore() + "/100)")
                .highestRated(best.getAirline())
                .fastest(fastest.getAirline() + " (" + formatDuration(fastest.getDurationMinutes()) + ")")
                .weights(weightMap)
                .explanation(explanation)
                .build();
    }

    private String formatDuration(int minutes) {
        int hrs = minutes / 60;
        int mins = minutes % 60;
        return mins > 0 ? String.format("%dh %dm", hrs, mins) : String.format("%dh", hrs);
    }
}
