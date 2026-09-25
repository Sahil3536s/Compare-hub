package com.comparehub.service.impl;

import com.comparehub.config.ComparisonRankingProperties;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.RankResultDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.service.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProductRankingServiceImpl implements ProductRankingService {

    private final ComparisonRankingProperties rankingConfig;

    @Autowired
    public ProductRankingServiceImpl(ComparisonRankingProperties rankingConfig) {
        this.rankingConfig = rankingConfig;
    }

    public ProductRankingServiceImpl(com.comparehub.config.RankingConfigProperties legacyProperties) {
        this(new ComparisonRankingProperties());
    }

    public ProductRankingServiceImpl() {
        this(new ComparisonRankingProperties());
    }

    @Override
    public List<NormalizedProductOfferDto> rankAndMarkCheapest(
            List<NormalizedProductOfferDto> offers, String sortBy) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        double wPrice = rankingConfig.getPriceWeight();
        double wRating = rankingConfig.getRatingWeight();
        double wDelivery = rankingConfig.getDeliveryWeight();
        double wDiscount = rankingConfig.getDiscountWeight();
        double wAvailability = rankingConfig.getAvailabilityWeight();

        // 1. Calculate price range for normalization (using effectiveCost where available)
        BigDecimal minPrice = offers.stream()
                .map(this::resolveComparablePrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = offers.stream()
                .map(this::resolveComparablePrice)
                .max(BigDecimal::compareTo)
                .orElse(minPrice);

        double priceRange = maxPrice.subtract(minPrice).doubleValue();

        // Calculate average price across all offers
        BigDecimal sumPrice = BigDecimal.ZERO;
        for (NormalizedProductOfferDto o : offers) {
            sumPrice = sumPrice.add(resolveComparablePrice(o));
        }
        BigDecimal avgPrice = sumPrice.divide(BigDecimal.valueOf(offers.size()), 2, RoundingMode.HALF_UP);

        // 2. Compute normalized metrics (0.0 to 1.0) and multi-factor scores
        for (NormalizedProductOfferDto offer : offers) {
            BigDecimal price = resolveComparablePrice(offer);

            // A. Price Score: lower price = better -> [0.0, 1.0]
            // Proportional relative ratio ensures fairness and stability across small and large offer sets
            double normPrice;
            if (price.compareTo(BigDecimal.ZERO) <= 0 || minPrice.compareTo(BigDecimal.ZERO) <= 0) {
                normPrice = 1.0;
            } else {
                normPrice = clamp(minPrice.doubleValue() / price.doubleValue());
            }

            // B. Rating Score: Bayesian review-count damping -> [0.0, 1.0]
            // Prevents 5.0 with 1 review from dominating 4.8 with 20,000 reviews
            double dampedRating = calculateDampedRating(offer.getRating(), offer.getReviewCount());
            double normRating = clamp(dampedRating / 5.0);

            // C. Delivery Score: lower delivery time = better -> [0.0, 1.0]
            // Unknown delivery is strictly NOT treated as fastest
            double normDelivery = calculateDeliveryScore(offer.getDelivery(), offer.getDeliveryEstimate());

            // D. Discount Score: higher discount = better -> [0.0, 1.0]
            int discountVal = offer.getDiscountPercentage() != null ? offer.getDiscountPercentage()
                    : (offer.getDiscountPercent() != null ? offer.getDiscountPercent() : 0);
            double normDiscount = clamp(discountVal / 50.0);

            // E. Availability Score: in-stock = 1.0, out of stock = 0.0
            boolean inStock = (offer.getInStock() != null && offer.getInStock())
                    || (offer.getAvailability() != null && offer.getAvailability());
            double normAvailability = inStock ? 1.0 : 0.0;

            // F. Multi-Factor Weighted Final Score
            double finalScore = (normPrice * wPrice)
                    + (normRating * wRating)
                    + (normDelivery * wDelivery)
                    + (normDiscount * wDiscount)
                    + (normAvailability * wAvailability);

            finalScore = Math.round(finalScore * 100.0) / 100.0;

            // Save individual scores on offer
            offer.setPriceScore(Math.round(normPrice * 100.0) / 100.0);
            offer.setRatingScore(Math.round(normRating * 100.0) / 100.0);
            offer.setDeliveryScore(Math.round(normDelivery * 100.0) / 100.0);
            offer.setDiscountScore(Math.round(normDiscount * 100.0) / 100.0);
            offer.setAvailabilityScore(Math.round(normAvailability * 100.0) / 100.0);
            offer.setFinalScore(finalScore);
            offer.setRankingScore(Math.round(finalScore * 100.0 * 10.0) / 10.0);
        }

        // 3. Identify special badge winners
        NormalizedProductOfferDto cheapestCandidate = offers.stream()
                .min(Comparator.comparing(this::resolveComparablePrice)
                        .thenComparing(o -> o.getRatingScore() != null ? o.getRatingScore() : 0.0, Comparator.reverseOrder()))
                .orElse(offers.get(0));

        NormalizedProductOfferDto bestValueCandidate = offers.stream()
                .max(Comparator.comparingDouble(o -> o.getFinalScore() != null ? o.getFinalScore() : 0.0))
                .orElse(offers.get(0));

        NormalizedProductOfferDto highestRatedCandidate = offers.stream()
                .filter(o -> o.getRating() != null)
                .max(Comparator.comparingDouble((NormalizedProductOfferDto o) -> calculateDampedRating(o.getRating(), o.getReviewCount()))
                        .thenComparing(o -> o.getReviewCount() != null ? o.getReviewCount() : 0, Comparator.reverseOrder()))
                .orElse(offers.get(0));

        NormalizedProductOfferDto fastestDeliveryCandidate = offers.stream()
                .filter(o -> isDeliveryKnown(o.getDelivery(), o.getDeliveryEstimate()))
                .max(Comparator.comparingDouble(o -> o.getDeliveryScore() != null ? o.getDeliveryScore() : 0.0))
                .orElse(null);

        // 4. Sort offers according to selected strategy
        List<NormalizedProductOfferDto> sorted = new ArrayList<>(offers);
        String strategy = sortBy != null ? sortBy.toLowerCase(Locale.ROOT).trim() : "best";

        switch (strategy) {
            case "cheapest":
            case "price_asc":
            case "price-asc":
            case "effective_price":
            case "effective_price_asc":
                sorted.sort(Comparator.comparing(this::resolveComparablePrice)
                        .thenComparing(o -> o.getRatingScore() != null ? o.getRatingScore() : 0.0, Comparator.reverseOrder()));
                break;

            case "rating":
            case "rating_desc":
            case "highest_rated":
            case "highest-rated":
                sorted.sort(Comparator.comparingDouble((NormalizedProductOfferDto o) -> calculateDampedRating(o.getRating(), o.getReviewCount())).reversed()
                        .thenComparing(this::resolveComparablePrice));
                break;

            case "fastest_delivery":
            case "fastest":
            case "delivery":
            case "delivery_asc":
                sorted.sort(Comparator.comparingDouble((NormalizedProductOfferDto o) -> o.getDeliveryScore() != null ? o.getDeliveryScore() : 0.0).reversed()
                        .thenComparing(this::resolveComparablePrice));
                break;

            case "discount":
            case "discount_desc":
                sorted.sort(Comparator.comparing((NormalizedProductOfferDto o) ->
                                o.getDiscountPercentage() != null ? o.getDiscountPercentage() : (o.getDiscountPercent() != null ? o.getDiscountPercent() : 0),
                        Comparator.reverseOrder()));
                break;

            case "best":
            case "best_value":
            case "best-value":
            default:
                sorted.sort(Comparator.comparingDouble((NormalizedProductOfferDto o) -> o.getFinalScore() != null ? o.getFinalScore() : 0.0).reversed()
                        .thenComparing(this::resolveComparablePrice));
                break;
        }

        // Find fastest delivery days among offers for comparative reasons
        int fastestDays = extractDeliveryDays(fastestDeliveryCandidate);

        // 5. Assign Rank, Badges, Explanations, and "Why this option?"
        for (int i = 0; i < sorted.size(); i++) {
            NormalizedProductOfferDto offer = sorted.get(i);
            int rank = i + 1;
            offer.setRank(rank);

            boolean isCheapest = offer.equals(cheapestCandidate)
                    || resolveComparablePrice(offer).compareTo(resolveComparablePrice(cheapestCandidate)) == 0;
            boolean isBestValue = offer.equals(bestValueCandidate);
            boolean isHighestRated = offer.equals(highestRatedCandidate) && offer.getRating() != null;
            boolean isFastestDelivery = fastestDeliveryCandidate != null && offer.equals(fastestDeliveryCandidate);

            offer.setIsCheapest(isCheapest);
            offer.setIsBestValue(isBestValue);
            offer.setIsHighestRated(isHighestRated);
            offer.setIsFastestDelivery(isFastestDelivery);

            // Determine ranking label prioritizing active strategy context
            String label = "OPTION";
            if (strategy.contains("cheap") || strategy.contains("price")) {
                if (isCheapest) {
                    label = "CHEAPEST";
                } else if (isBestValue) {
                    label = "BEST_VALUE";
                } else if (isHighestRated) {
                    label = "HIGHEST_RATED";
                } else if (isFastestDelivery) {
                    label = "FASTEST_DELIVERY";
                }
            } else if (strategy.contains("rate") || strategy.contains("rating")) {
                if (isHighestRated) {
                    label = "HIGHEST_RATED";
                } else if (isBestValue) {
                    label = "BEST_VALUE";
                } else if (isCheapest) {
                    label = "CHEAPEST";
                } else if (isFastestDelivery) {
                    label = "FASTEST_DELIVERY";
                }
            } else if (strategy.contains("fast") || strategy.contains("deliv")) {
                if (isFastestDelivery) {
                    label = "FASTEST_DELIVERY";
                } else if (isBestValue) {
                    label = "BEST_VALUE";
                } else if (isCheapest) {
                    label = "CHEAPEST";
                } else if (isHighestRated) {
                    label = "HIGHEST_RATED";
                }
            } else {
                if (isBestValue) {
                    label = "BEST_VALUE";
                } else if (isCheapest) {
                    label = "CHEAPEST";
                } else if (isHighestRated) {
                    label = "HIGHEST_RATED";
                } else if (isFastestDelivery) {
                    label = "FASTEST_DELIVERY";
                }
            }
            offer.setRankingLabel(label);

            // Generate deterministic explanation
            String explanation = generateExplanation(offer, offers, avgPrice, label, isCheapest, isBestValue, isFastestDelivery, isHighestRated, fastestDays);
            offer.setRecommendationReason(explanation);

            // Generate "Why this option?" bullet points
            List<String> whyThisOption = generateWhyThisOptionReasons(offer, avgPrice, isCheapest);
            offer.setWhyThisOption(whyThisOption);

            // Build RankResultDto
            RankResultDto rankResult = RankResultDto.builder()
                    .rank(rank)
                    .score(offer.getFinalScore())
                    .label(label)
                    .explanation(explanation)
                    .reasons(whyThisOption)
                    .priceScore(offer.getPriceScore())
                    .ratingScore(offer.getRatingScore())
                    .deliveryScore(offer.getDeliveryScore())
                    .discountScore(offer.getDiscountScore())
                    .availabilityScore(offer.getAvailabilityScore())
                    .finalScore(offer.getFinalScore())
                    .build();

            offer.setRankResult(rankResult);
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

        NormalizedProductOfferDto fastest = rankedOffers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsFastestDelivery()))
                .findFirst()
                .orElse(null);

        Map<String, Double> weightsMap = new HashMap<>();
        weightsMap.put("price", rankingConfig.getPriceWeight());
        weightsMap.put("rating", rankingConfig.getRatingWeight());
        weightsMap.put("delivery", rankingConfig.getDeliveryWeight());
        weightsMap.put("discount", rankingConfig.getDiscountWeight());
        weightsMap.put("availability", rankingConfig.getAvailabilityWeight());

        String fastestDesc = fastest != null
                ? fastest.getMerchant() + " (" + (fastest.getDelivery() != null ? fastest.getDelivery() : "Fastest") + ")"
                : "Standard Delivery";

        return RankingSummaryDto.builder()
                .cheapest(cheapest.getMerchant() + " (₹" + resolveComparablePrice(cheapest).toPlainString() + ")")
                .bestValue(bestValue.getMerchant() + " (Score: " + bestValue.getFinalScore() + "/1.0)")
                .highestRated(highestRated.getMerchant() + " (" + (highestRated.getRating() != null ? highestRated.getRating() : "N/A") + "★)")
                .fastest(fastestDesc)
                .weights(weightsMap)
                .explanation("Multi-factor ranking weights: Price (" + (int)(rankingConfig.getPriceWeight() * 100)
                        + "%), Rating (" + (int)(rankingConfig.getRatingWeight() * 100)
                        + "%), Delivery (" + (int)(rankingConfig.getDeliveryWeight() * 100)
                        + "%), Discount (" + (int)(rankingConfig.getDiscountWeight() * 100)
                        + "%), Availability (" + (int)(rankingConfig.getAvailabilityWeight() * 100) + "%)")
                .build();
    }

    private BigDecimal resolveComparablePrice(NormalizedProductOfferDto offer) {
        if (offer == null) return BigDecimal.ZERO;
        if (offer.getEffectiveCost() != null) return offer.getEffectiveCost();
        if (offer.getEffectivePrice() != null) return offer.getEffectivePrice();
        if (offer.getCurrentPrice() != null) return offer.getCurrentPrice();
        if (offer.getPrice() != null) return offer.getPrice();
        return BigDecimal.ZERO;
    }

    /**
     * Bayesian rating damping formula:
     * (v * R + C * m) / (v + C)
     * where v = review count, R = rating, C = confidence threshold (50), m = prior expected rating (3.5)
     */
    public double calculateDampedRating(Double rating, Integer reviewCount) {
        if (rating == null) {
            // Missing rating is treated as neutral baseline 2.5/5.0, NEVER replaced with 5.0!
            return 2.5;
        }

        double prior = rankingConfig.getPriorRating();
        double c = rankingConfig.getConfidenceThreshold();
        double v = (reviewCount != null && reviewCount >= 0) ? reviewCount.doubleValue() : 10.0;

        return (v * rating + c * prior) / (v + c);
    }

    /**
     * Calculates delivery score between 0.0 and 1.0.
     * Unknown delivery is strictly NOT treated as fastest (score = 0.20).
     */
    public double calculateDeliveryScore(String delivery, String estimate) {
        String text = delivery != null && !delivery.isBlank() ? delivery : estimate;
        if (text == null || text.isBlank()) {
            return 0.20; // Unknown delivery
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("same day") || lower.contains("today")) {
            return 1.0;
        }
        if (lower.contains("tomorrow") || lower.contains("1 day") || lower.contains("next day")) {
            return 0.85;
        }
        if (lower.contains("2 day") || lower.contains("2-3 day") || lower.contains("express")) {
            return 0.70;
        }
        if (lower.contains("3-5 day") || lower.contains("4 day")) {
            return 0.55;
        }
        if (lower.contains("free delivery") || lower.contains("free shipping")) {
            return 0.65;
        }
        if (lower.contains("standard") || lower.contains("courier")) {
            return 0.40;
        }

        return 0.35;
    }

    private boolean isDeliveryKnown(String delivery, String estimate) {
        String text = delivery != null && !delivery.isBlank() ? delivery : estimate;
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("same day") || lower.contains("today") || lower.contains("tomorrow")
                || lower.contains("day") || lower.contains("express") || lower.contains("free");
    }

    private int extractDeliveryDays(NormalizedProductOfferDto offer) {
        if (offer == null) return 5;
        String text = offer.getDelivery() != null ? offer.getDelivery() : offer.getDeliveryEstimate();
        if (text == null) return 5;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("same day") || lower.contains("today")) return 0;
        if (lower.contains("tomorrow") || lower.contains("1 day") || lower.contains("next day")) return 1;
        if (lower.contains("2 day")) return 2;
        if (lower.contains("3 day")) return 3;
        Matcher m = Pattern.compile("(\\d+)\\s*day").matcher(lower);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 4;
    }

    private String generateExplanation(
            NormalizedProductOfferDto offer,
            List<NormalizedProductOfferDto> offers,
            BigDecimal avgPrice,
            String label,
            boolean isCheapest,
            boolean isBestValue,
            boolean isFastestDelivery,
            boolean isHighestRated,
            int fastestDays) {

        BigDecimal price = resolveComparablePrice(offer);
        String deliveryText = offer.getDelivery() != null ? offer.getDelivery() : "standard delivery";

        if ("CHEAPEST".equals(label)) {
            boolean hasDeliveryKnown = offer.getEffectiveCost() != null || offer.getEffectivePrice() != null
                    || offer.getDeliveryCost() != null || (offer.getDelivery() != null && offer.getDelivery().toLowerCase(Locale.ROOT).contains("free"));
            if (hasDeliveryKnown) {
                return "Lowest known effective cost among " + offers.size() + " available offers.";
            } else {
                return "Lowest displayed price among " + offers.size() + " available offers.";
            }
        }

        if ("FASTEST_DELIVERY".equals(label)) {
            int thisDays = extractDeliveryDays(offer);
            int nextBestDays = offers.stream()
                    .filter(o -> !o.equals(offer))
                    .mapToInt(this::extractDeliveryDays)
                    .min()
                    .orElse(thisDays + 1);

            int daysEarlier = Math.max(1, nextBestDays - thisDays);
            if (daysEarlier > 0 && nextBestDays > thisDays) {
                return "Estimated delivery is " + daysEarlier + " day(s) earlier than the next available option.";
            }
            return "Fastest confirmed delivery (" + deliveryText + ") among all stores.";
        }

        if ("HIGHEST_RATED".equals(label)) {
            String count = offer.getReviewCount() != null ? " across " + offer.getReviewCount() + " reviews" : "";
            return "Top customer satisfaction with " + offer.getRating() + "/5 rating" + count + ".";
        }

        if ("BEST_VALUE".equals(label) || isBestValue) {
            if (price.compareTo(avgPrice) < 0) {
                BigDecimal diff = avgPrice.subtract(price).setScale(0, RoundingMode.HALF_UP);
                String ratingPart = offer.getRating() != null ? "rated " + offer.getRating() + "/5" : "well reviewed";
                String deliveryPart = deliveryText.toLowerCase(Locale.ROOT).contains("free") ? "includes free delivery" : "fast delivery";
                return String.format("₹%s below the average offer, %s and %s.",
                        diff.toPlainString(), ratingPart, deliveryPart);
            } else {
                return String.format("Highest composite value score (%s/1.0) with verified %s★ rating and confirmed stock.",
                        offer.getFinalScore(), offer.getRating() != null ? offer.getRating() : "4.5");
            }
        }

        if (isCheapest) {
            boolean hasDeliveryKnown = offer.getEffectiveCost() != null || offer.getEffectivePrice() != null
                    || offer.getDeliveryCost() != null || (offer.getDelivery() != null && offer.getDelivery().toLowerCase(Locale.ROOT).contains("free"));
            if (hasDeliveryKnown) {
                return "Lowest known effective cost among " + offers.size() + " available offers.";
            } else {
                return "Lowest displayed price among " + offers.size() + " available offers.";
            }
        }

        if (isFastestDelivery) {
            int thisDays = extractDeliveryDays(offer);
            int nextBestDays = offers.stream()
                    .filter(o -> !o.equals(offer))
                    .mapToInt(this::extractDeliveryDays)
                    .min()
                    .orElse(thisDays + 1);

            int daysEarlier = Math.max(1, nextBestDays - thisDays);
            if (daysEarlier > 0 && nextBestDays > thisDays) {
                return "Estimated delivery is " + daysEarlier + " day(s) earlier than the next available option.";
            }
            return "Fastest confirmed delivery (" + deliveryText + ") among all stores.";
        }

        if (isHighestRated) {
            String count = offer.getReviewCount() != null ? " across " + offer.getReviewCount() + " reviews" : "";
            return "Top customer satisfaction with " + offer.getRating() + "/5 rating" + count + ".";
        }

        return "Verified merchant offer with reliable pricing and stock availability.";
    }

    private List<String> generateWhyThisOptionReasons(
            NormalizedProductOfferDto offer,
            BigDecimal avgPrice,
            boolean isCheapest) {

        List<String> reasons = new ArrayList<>();
        BigDecimal price = resolveComparablePrice(offer);

        if (isCheapest) {
            reasons.add("Lowest effective price");
        } else if (price.compareTo(avgPrice) < 0) {
            BigDecimal diff = avgPrice.subtract(price).setScale(0, RoundingMode.HALF_UP);
            reasons.add("₹" + diff.toPlainString() + " below average offer");
        }

        if (offer.getRating() != null) {
            String rev = offer.getReviewCount() != null ? " (" + offer.getReviewCount() + " reviews)" : "";
            reasons.add(offer.getRating() + "/5 rating" + rev);
        }

        if (offer.getDelivery() != null && !offer.getDelivery().isBlank()) {
            String d = offer.getDelivery().toLowerCase(Locale.ROOT);
            if (d.contains("free")) {
                reasons.add("Free delivery");
            } else if (d.contains("same day") || d.contains("today")) {
                reasons.add("Same-day delivery");
            } else if (d.contains("tomorrow") || d.contains("1 day")) {
                reasons.add("Next-day delivery");
            }
        }

        if (Boolean.TRUE.equals(offer.getInStock()) || Boolean.TRUE.equals(offer.getAvailability())) {
            reasons.add("In stock");
        }

        int discount = offer.getDiscountPercentage() != null ? offer.getDiscountPercentage()
                : (offer.getDiscountPercent() != null ? offer.getDiscountPercent() : 0);
        if (discount >= 10) {
            reasons.add(discount + "% discount off MRP");
        }

        return reasons;
    }

    private double clamp(double val) {
        return Math.min(1.0, Math.max(0.0, val));
    }
}
