package com.comparehub.service.impl;

import com.comparehub.config.RankingConfigProperties;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.ProductRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRankingServiceImpl implements ProductRankingService {

    private final RankingConfigProperties rankingConfigProperties;

    @Override
    public List<NormalizedProductOfferDto> rankAndMarkCheapest(
            List<NormalizedProductOfferDto> offers, String sortBy) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        RankingConfigProperties.ProductWeights weights = rankingConfigProperties.getProduct();
        double wPrice = weights.getPriceWeight();
        double wRating = weights.getRatingWeight();
        double wDiscount = weights.getDiscountWeight();
        double wDelivery = weights.getDeliveryWeight();
        double wTrust = weights.getTrustWeight();

        // 1. Determine min and max for numerical normalization (using effectivePrice if available)
        BigDecimal minPrice = offers.stream()
                .map(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = offers.stream()
                .map(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice())
                .max(BigDecimal::compareTo)
                .orElse(minPrice);

        double priceRange = maxPrice.subtract(minPrice).doubleValue();

        double minRating = offers.stream().mapToDouble(o -> o.getRating() != null ? o.getRating() : 3.0).min().orElse(3.0);
        double maxRating = offers.stream().mapToDouble(o -> o.getRating() != null ? o.getRating() : 3.0).max().orElse(5.0);
        double ratingRange = maxRating - minRating;

        double minDiscount = offers.stream().mapToDouble(o -> o.getDiscountPercent() != null ? o.getDiscountPercent() : 0).min().orElse(0);
        double maxDiscount = offers.stream().mapToDouble(o -> o.getDiscountPercent() != null ? o.getDiscountPercent() : 0).max().orElse(10);
        double discountRange = maxDiscount - minDiscount;

        // 2. Normalize and compute deterministic Best Value score
        double highestScore = -1.0;
        NormalizedProductOfferDto bestValueCandidate = null;
        double maxRatingFound = -1.0;
        NormalizedProductOfferDto highestRatedCandidate = null;

        for (NormalizedProductOfferDto offer : offers) {
            BigDecimal effectiveOrPrice = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();

            // Price: lower price is better -> [0.0, 1.0]
            double normPrice = (priceRange > 0)
                    ? (maxPrice.subtract(effectiveOrPrice).doubleValue()) / priceRange
                    : 1.0;

            // Rating: higher is better -> [0.0, 1.0]
            double ratingVal = offer.getRating() != null ? offer.getRating() : 3.0;
            double normRating = (ratingRange > 0)
                    ? (ratingVal - minRating) / ratingRange
                    : (ratingVal / 5.0);

            // Discount: higher is better -> [0.0, 1.0]
            double discountVal = offer.getDiscountPercent() != null ? offer.getDiscountPercent() : 0;
            double normDiscount = (discountRange > 0)
                    ? (discountVal - minDiscount) / discountRange
                    : (discountVal / 100.0);

            // Delivery: normalized based on speed
            double normDelivery = calculateDeliveryScore(offer.getDelivery());

            // Availability & Trust
            double normTrust = (Boolean.TRUE.equals(offer.getInStock()) ? 0.7 : 0.0)
                    + (isTopMerchant(offer.getMerchant()) ? 0.3 : 0.15);

            // Composite Score
            double score = (normPrice * wPrice)
                    + (normRating * wRating)
                    + (normDiscount * wDiscount)
                    + (normDelivery * wDelivery)
                    + (normTrust * wTrust);

            BigDecimal roundedScore = BigDecimal.valueOf(score * 100.0).setScale(1, RoundingMode.HALF_UP);
            offer.setRankingScore(roundedScore.doubleValue());

            if (score > highestScore) {
                highestScore = score;
                bestValueCandidate = offer;
            }

            if (ratingVal > maxRatingFound) {
                maxRatingFound = ratingVal;
                highestRatedCandidate = offer;
            }
        }

        // 3. Mark Badges (Cheapest, Best Value, Highest Rated)
        for (NormalizedProductOfferDto offer : offers) {
            BigDecimal effectiveOrPrice = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();
            offer.setIsCheapest(effectiveOrPrice.compareTo(minPrice) == 0);
            offer.setIsBestValue(bestValueCandidate != null && offer.equals(bestValueCandidate));
            offer.setIsHighestRated(highestRatedCandidate != null && offer.equals(highestRatedCandidate));
        }

        // 4. Sort offers according to requested sortBy strategy
        List<NormalizedProductOfferDto> sorted = new ArrayList<>(offers);
        String strategy = sortBy != null ? sortBy.toLowerCase().trim() : "best";

        switch (strategy) {
            case "effective_price_asc":
            case "effective-price-asc":
            case "effective_price":
                sorted.sort(Comparator.comparing(o -> o.getEffectivePrice() != null ? o.getEffectivePrice() : o.getPrice()));
                break;
            case "price_asc":
            case "price-asc":
                sorted.sort(Comparator.comparing(NormalizedProductOfferDto::getPrice));
                break;
            case "price_desc":
            case "price-desc":
                sorted.sort(Comparator.comparing(NormalizedProductOfferDto::getPrice).reversed());
                break;
            case "rating":
            case "rating_desc":
                sorted.sort(Comparator.comparing(NormalizedProductOfferDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "discount":
            case "discount_desc":
                sorted.sort(Comparator.comparing(NormalizedProductOfferDto::getDiscountPercent, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "best":
            case "best_value":
            default:
                sorted.sort(Comparator.comparing(NormalizedProductOfferDto::getRankingScore, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
        }

        return sorted;
    }

    @Override
    public RankingSummaryDto getRankingSummary(List<NormalizedProductOfferDto> rankedOffers) {
        if (rankedOffers == null || rankedOffers.isEmpty()) {
            return RankingSummaryDto.builder().build();
        }

        NormalizedProductOfferDto cheapest = rankedOffers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCheapest()))
                .findFirst()
                .orElse(rankedOffers.get(0));

        NormalizedProductOfferDto bestValue = rankedOffers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsBestValue()))
                .findFirst()
                .orElse(rankedOffers.get(0));

        NormalizedProductOfferDto highestRated = rankedOffers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsHighestRated()))
                .findFirst()
                .orElse(rankedOffers.get(0));

        RankingConfigProperties.ProductWeights weightsConfig = rankingConfigProperties.getProduct();
        Map<String, Double> weightsMap = new HashMap<>();
        weightsMap.put("price", weightsConfig.getPriceWeight());
        weightsMap.put("rating", weightsConfig.getRatingWeight());
        weightsMap.put("discount", weightsConfig.getDiscountWeight());
        weightsMap.put("delivery", weightsConfig.getDeliveryWeight());
        weightsMap.put("trust", weightsConfig.getTrustWeight());

        return RankingSummaryDto.builder()
                .cheapest(cheapest.getMerchant() + " (₹" + (cheapest.getEffectivePrice() != null ? cheapest.getEffectivePrice() : cheapest.getPrice()) + ")")
                .bestValue(bestValue.getMerchant() + " (Score: " + bestValue.getRankingScore() + "/100)")
                .highestRated(highestRated.getMerchant() + " (" + highestRated.getRating() + "★)")
                .weights(weightsMap)
                .explanation("Ranked using deterministic weights: Price (" + (int)(weightsConfig.getPriceWeight()*100) + "%), Rating (" + (int)(weightsConfig.getRatingWeight()*100) + "%), Discount (" + (int)(weightsConfig.getDiscountWeight()*100) + "%), Delivery (" + (int)(weightsConfig.getDeliveryWeight()*100) + "%), Trust (" + (int)(weightsConfig.getTrustWeight()*100) + "%)")
                .build();
    }

    private double calculateDeliveryScore(String delivery) {
        if (delivery == null || delivery.isBlank()) return 0.5;
        String d = delivery.toLowerCase();
        if (d.contains("same day") || d.contains("today")) return 1.0;
        if (d.contains("tomorrow") || d.contains("1 day")) return 0.85;
        if (d.contains("2 day") || d.contains("2-3 days") || d.contains("express")) return 0.70;
        if (d.contains("free")) return 0.60;
        return 0.40;
    }

    private boolean isTopMerchant(String merchant) {
        if (merchant == null) return false;
        String m = merchant.toLowerCase();
        return m.contains("amazon") || m.contains("flipkart") || m.contains("croma") || m.contains("official");
    }
}
