package com.comparehub.service.impl;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.RideRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RideRankingServiceImpl implements RideRankingService {

    private final RankingConfigProperties rankingConfigProperties;

    @Override
    public List<NormalizedRideOfferDto> rankAndBadgeRides(
            List<NormalizedRideOfferDto> offers, String sortBy) {

        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        RankingConfigProperties.RideWeights weights = rankingConfigProperties.getRide();
        double wFare = weights.getFareWeight();
        double wEta = weights.getEtaWeight();

        // 1. Min/Max bounds for normalization
        BigDecimal minFare = offers.stream().map(NormalizedRideOfferDto::getEstimatedPriceMin).min(BigDecimal::compareTo).orElse(BigDecimal.valueOf(50));
        BigDecimal maxFare = offers.stream().map(NormalizedRideOfferDto::getEstimatedPriceMin).max(BigDecimal::compareTo).orElse(minFare);
        double fareRange = maxFare.subtract(minFare).doubleValue();

        int minEta = offers.stream().mapToInt(NormalizedRideOfferDto::getEtaMinutes).min().orElse(2);
        int maxEta = offers.stream().mapToInt(NormalizedRideOfferDto::getEtaMinutes).max().orElse(minEta);
        double etaRange = maxEta - minEta;

        // 2. Normalize and compute deterministic Best Value score: Fare (60%), ETA (40%)
        double highestScore = -1.0;
        NormalizedRideOfferDto bestCandidate = null;

        for (NormalizedRideOfferDto offer : offers) {
            // Fare: lower is better -> [0.0, 1.0]
            double normFare = (fareRange > 0)
                    ? (maxFare.subtract(offer.getEstimatedPriceMin()).doubleValue()) / fareRange
                    : 1.0;

            // ETA: lower is better -> [0.0, 1.0]
            double normEta = (etaRange > 0)
                    ? ((double) (maxEta - offer.getEtaMinutes())) / etaRange
                    : 1.0;

            double score = (normFare * wFare) + (normEta * wEta);
            BigDecimal roundedScore = BigDecimal.valueOf(score * 100.0).setScale(1, RoundingMode.HALF_UP);
            offer.setScore(roundedScore.doubleValue());

            if (score > highestScore) {
                highestScore = score;
                bestCandidate = offer;
            }
        }

        // 3. Mark Badges
        for (NormalizedRideOfferDto offer : offers) {
            offer.setIsCheapest(offer.getEstimatedPriceMin().compareTo(minFare) == 0);
            offer.setIsFastest(offer.getEtaMinutes() == minEta);
            offer.setIsBest(bestCandidate != null && offer.equals(bestCandidate));
        }

        // 4. Sort Strategy
        List<NormalizedRideOfferDto> sorted = new ArrayList<>(offers);
        String strategy = sortBy != null ? sortBy.toLowerCase().trim() : "best";

        switch (strategy) {
            case "cheapest":
            case "price_asc":
            case "price-asc":
                sorted.sort(Comparator.comparing(NormalizedRideOfferDto::getEstimatedPriceMin));
                break;
            case "fastest":
            case "eta":
                sorted.sort(Comparator.comparing(NormalizedRideOfferDto::getEtaMinutes)
                        .thenComparing(NormalizedRideOfferDto::getEstimatedPriceMin));
                break;
            case "best":
            default:
                sorted.sort(Comparator.comparing(NormalizedRideOfferDto::getScore).reversed());
                break;
        }

        return sorted;
    }

    @Override
    public RankingSummaryDto getRankingSummary(List<NormalizedRideOfferDto> rankedOffers) {
        if (rankedOffers == null || rankedOffers.isEmpty()) {
            return RankingSummaryDto.builder()
                    .cheapest("N/A")
                    .bestValue("N/A")
                    .highestRated("N/A")
                    .fastest("N/A")
                    .explanation("No rides to rank.")
                    .build();
        }

        RankingConfigProperties.RideWeights weights = rankingConfigProperties.getRide();
        Map<String, Double> weightMap = Map.of(
                "fare", weights.getFareWeight(),
                "eta", weights.getEtaWeight()
        );

        NormalizedRideOfferDto cheapest = rankedOffers.stream().filter(NormalizedRideOfferDto::getIsCheapest).findFirst().orElse(rankedOffers.get(0));
        NormalizedRideOfferDto best = rankedOffers.stream().filter(NormalizedRideOfferDto::getIsBest).findFirst().orElse(rankedOffers.get(0));
        NormalizedRideOfferDto fastest = rankedOffers.stream().filter(NormalizedRideOfferDto::getIsFastest).findFirst().orElse(rankedOffers.get(0));

        String explanation = String.format(
                "Best ride value is calculated deterministically: Estimated Fare (%d%%), Driver Pickup ETA (%d%%).",
                Math.round(weights.getFareWeight() * 100),
                Math.round(weights.getEtaWeight() * 100)
        );

        return RankingSummaryDto.builder()
                .cheapest(cheapest.getProvider() + " " + cheapest.getRideType() + " (₹" + cheapest.getEstimatedPriceMin() + ")")
                .bestValue(best.getProvider() + " " + best.getRideType() + " (Score: " + best.getScore() + "/100)")
                .highestRated(best.getProvider())
                .fastest(fastest.getProvider() + " " + fastest.getRideType() + " (" + fastest.getEtaMinutes() + " mins)")
                .weights(weightMap)
                .explanation(explanation)
                .build();
    }
}
