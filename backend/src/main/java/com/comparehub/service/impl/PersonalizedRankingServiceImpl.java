package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.User;
import com.comparehub.model.UserRankingPreference;
import com.comparehub.repository.UserRepository;
import com.comparehub.repository.UserRankingPreferenceRepository;
import com.comparehub.service.PersonalizedRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizedRankingServiceImpl implements PersonalizedRankingService {

    private final UserRankingPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserRankingPreferenceDto getUserPreferences(Long userId) {
        if (userId == null) {
            return getDefaultPreferences(null);
        }

        return preferenceRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseGet(() -> getDefaultPreferences(userId));
    }

    @Override
    @Transactional
    public UserRankingPreferenceDto saveUserPreferences(Long userId, UserRankingPreferenceDto dto) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to persist preferences");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Normalize weights before persisting
        if (dto.getProduct() != null) dto.getProduct().normalizeWeights();
        if (dto.getFlight() != null) dto.getFlight().normalizeWeights();
        if (dto.getRide() != null) dto.getRide().normalizeWeights();

        UserRankingPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserRankingPreference.builder().user(user).build());

        preference.setPreset(dto.getPreset() != null ? dto.getPreset() : "CUSTOM");

        if (dto.getProduct() != null) {
            preference.setProductPrice(dto.getProduct().getPrice());
            preference.setProductRating(dto.getProduct().getRating());
            preference.setProductDiscount(dto.getProduct().getDiscount());
            preference.setProductDelivery(dto.getProduct().getDelivery());
            preference.setProductReliability(dto.getProduct().getReliability());
        }

        if (dto.getFlight() != null) {
            preference.setFlightPrice(dto.getFlight().getPrice());
            preference.setFlightDuration(dto.getFlight().getDuration());
            preference.setFlightStops(dto.getFlight().getStops());
        }

        if (dto.getRide() != null) {
            preference.setRideFare(dto.getRide().getFare());
            preference.setRideEta(dto.getRide().getEta());
        }

        UserRankingPreference saved = preferenceRepository.save(preference);
        log.info("Saved personalized ranking preferences for user: {}", userId);
        return mapToDto(saved);
    }

    @Override
    public List<NormalizedProductOfferDto> rankProductsWithCustomWeights(
            List<NormalizedProductOfferDto> offers, ProductRankingWeightsDto weights, String sortBy) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        ProductRankingWeightsDto w = weights != null ? weights : new ProductRankingWeightsDto();
        w.normalizeWeights();

        double wPrice = w.getPrice() / 100.0;
        double wRating = w.getRating() / 100.0;
        double wDiscount = w.getDiscount() / 100.0;
        double wDelivery = w.getDelivery() / 100.0;
        double wTrust = w.getReliability() / 100.0;

        // 1. Min/Max normalization values
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

        double highestScore = -1.0;
        NormalizedProductOfferDto bestValueCandidate = null;
        double maxRatingFound = -1.0;
        NormalizedProductOfferDto highestRatedCandidate = null;

        for (NormalizedProductOfferDto offer : offers) {
            BigDecimal effectiveOrPrice = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();

            double normPrice = (priceRange > 0)
                    ? (maxPrice.subtract(effectiveOrPrice).doubleValue()) / priceRange
                    : 1.0;

            double ratingVal = offer.getRating() != null ? offer.getRating() : 3.0;
            double normRating = (ratingRange > 0)
                    ? (ratingVal - minRating) / ratingRange
                    : (ratingVal / 5.0);

            double discountVal = offer.getDiscountPercent() != null ? offer.getDiscountPercent() : 0;
            double normDiscount = (discountRange > 0)
                    ? (discountVal - minDiscount) / discountRange
                    : (discountVal / 100.0);

            double normDelivery = calculateDeliveryScore(offer.getDelivery());

            double normTrust = (Boolean.TRUE.equals(offer.getInStock()) ? 0.7 : 0.0)
                    + (isTopMerchant(offer.getMerchant()) ? 0.3 : 0.15);

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

        // 3. Mark Badges
        for (NormalizedProductOfferDto offer : offers) {
            BigDecimal effectiveOrPrice = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();
            offer.setIsCheapest(effectiveOrPrice.compareTo(minPrice) == 0);
            offer.setIsBestValue(bestValueCandidate != null && offer.equals(bestValueCandidate));
            offer.setIsHighestRated(highestRatedCandidate != null && offer.equals(highestRatedCandidate));
        }

        // 4. Sort
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
    public RankingSummaryDto getPersonalizedRankingSummary(
            List<NormalizedProductOfferDto> rankedOffers, ProductRankingWeightsDto weights) {
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

        ProductRankingWeightsDto w = weights != null ? weights : new ProductRankingWeightsDto();
        Map<String, Double> weightsMap = new HashMap<>();
        weightsMap.put("price", (double) w.getPrice() / 100.0);
        weightsMap.put("rating", (double) w.getRating() / 100.0);
        weightsMap.put("discount", (double) w.getDiscount() / 100.0);
        weightsMap.put("delivery", (double) w.getDelivery() / 100.0);
        weightsMap.put("trust", (double) w.getReliability() / 100.0);

        String exp = String.format("Personalized Ranking: Price (%d%%), Rating (%d%%), Discount (%d%%), Delivery (%d%%), Reliability (%d%%)",
                w.getPrice(), w.getRating(), w.getDiscount(), w.getDelivery(), w.getReliability());

        return RankingSummaryDto.builder()
                .cheapest(cheapest.getMerchant() + " (₹" + (cheapest.getEffectivePrice() != null ? cheapest.getEffectivePrice() : cheapest.getPrice()) + ")")
                .bestValue(bestValue.getMerchant() + " (Score: " + bestValue.getRankingScore() + "/100)")
                .highestRated(highestRated.getMerchant() + " (" + highestRated.getRating() + "★)")
                .weights(weightsMap)
                .explanation(exp)
                .build();
    }

    private UserRankingPreferenceDto getDefaultPreferences(Long userId) {
        return UserRankingPreferenceDto.builder()
                .userId(userId)
                .preset("BALANCED")
                .product(ProductRankingWeightsDto.builder()
                        .price(40)
                        .rating(20)
                        .discount(15)
                        .delivery(15)
                        .reliability(10)
                        .build())
                .flight(FlightRankingWeightsDto.builder()
                        .price(50)
                        .duration(30)
                        .stops(20)
                        .build())
                .ride(RideRankingWeightsDto.builder()
                        .fare(60)
                        .eta(40)
                        .build())
                .build();
    }

    private UserRankingPreferenceDto mapToDto(UserRankingPreference entity) {
        return UserRankingPreferenceDto.builder()
                .userId(entity.getUser().getId())
                .preset(entity.getPreset())
                .product(ProductRankingWeightsDto.builder()
                        .price(entity.getProductPrice())
                        .rating(entity.getProductRating())
                        .discount(entity.getProductDiscount())
                        .delivery(entity.getProductDelivery())
                        .reliability(entity.getProductReliability())
                        .build())
                .flight(FlightRankingWeightsDto.builder()
                        .price(entity.getFlightPrice())
                        .duration(entity.getFlightDuration())
                        .stops(entity.getFlightStops())
                        .build())
                .ride(RideRankingWeightsDto.builder()
                        .fare(entity.getRideFare())
                        .eta(entity.getRideEta())
                        .build())
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
