package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.provider.ProductProvider;
import com.comparehub.service.ProductAlternativeService;
import com.comparehub.service.ProductFeatureComparisonService;
import com.comparehub.service.ProductMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAlternativeServiceImpl implements ProductAlternativeService {

    private final List<ProductProvider> providers;
    private final ProductMatchingService productMatchingService;
    private final ProductFeatureComparisonService featureComparisonService;

    @Override
    public ProductAlternativesResponseDto getAlternatives(
            String productName,
            BigDecimal price,
            String category,
            String brand,
            int limit) {

        String safeName = productName != null ? productName.trim() : "Target Product";
        BigDecimal safePrice = price != null && price.compareTo(BigDecimal.ZERO) > 0 ? price : new BigDecimal("50000");
        String safeCategory = category != null && !category.isBlank() ? category.trim() : "Smartphones";
        int safeLimit = limit > 0 ? limit : 4;

        NormalizedProductOfferDto baseOffer = NormalizedProductOfferDto.builder()
                .productName(safeName)
                .price(safePrice)
                .category(safeCategory)
                .brand(brand != null ? brand : "Apple")
                .rating(4.7)
                .inStock(true)
                .build();

        // 1. Enrich base offer with attributes
        baseOffer = productMatchingService.enrichWithMatching(List.of(baseOffer)).get(0);

        // 2. Fetch catalog items for category
        List<NormalizedProductOfferDto> catalog = new ArrayList<>();
        for (ProductProvider provider : providers) {
            try {
                List<NormalizedProductOfferDto> results = provider.searchProducts(safeCategory);
                if (results != null && !results.isEmpty()) {
                    catalog.addAll(results);
                }
            } catch (Exception e) {
                log.warn("Provider {} failed fetching alternatives catalog: {}", provider.getProviderName(), e.getMessage());
            }
        }

        // Also search by category name or generic keyword if catalog is limited
        if (catalog.size() < 4) {
            for (ProductProvider provider : providers) {
                try {
                    List<NormalizedProductOfferDto> genericResults = provider.searchProducts("");
                    if (genericResults != null) {
                        catalog.addAll(genericResults);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. Enrich catalog with structured attributes
        List<NormalizedProductOfferDto> enrichedCatalog = productMatchingService.enrichWithMatching(catalog);

        // 4. Find and categorize alternatives
        List<ProductAlternativeDto> alternatives = findAlternativesForOffer(baseOffer, enrichedCatalog, safeLimit);

        String summary = String.format("Found %d compelling alternatives for %s across price and feature tiers.",
                alternatives.size(), safeName);

        return ProductAlternativesResponseDto.builder()
                .baseProductName(safeName)
                .basePrice(safePrice)
                .baseCategory(safeCategory)
                .baseBrand(brand)
                .totalAlternatives(alternatives.size())
                .alternatives(alternatives)
                .summary(summary)
                .build();
    }

    @Override
    public List<ProductAlternativeDto> findAlternativesForOffer(
            NormalizedProductOfferDto baseOffer,
            List<NormalizedProductOfferDto> availableOffers,
            int limit) {

        if (baseOffer == null || availableOffers == null || availableOffers.isEmpty()) {
            return new ArrayList<>();
        }

        int maxResults = limit > 0 ? limit : 4;
        String baseCanonical = baseOffer.getCanonicalKey() != null ? baseOffer.getCanonicalKey() : "";
        String baseTitleNorm = normalize(baseOffer.getProductName());
        BigDecimal basePrice = baseOffer.getPrice() != null ? baseOffer.getPrice() : BigDecimal.ZERO;
        String baseCat = baseOffer.getCategory() != null ? baseOffer.getCategory().toLowerCase() : "";

        // Filter out identical products or exact duplicates of the same canonical key
        Set<String> seenKeys = new HashSet<>();
        if (!baseCanonical.isBlank()) seenKeys.add(baseCanonical);
        if (!baseTitleNorm.isBlank()) seenKeys.add(baseTitleNorm);

        List<ProductAlternativeDto> candidateList = new ArrayList<>();

        for (NormalizedProductOfferDto candidate : availableOffers) {
            if (candidate == null || candidate.getPrice() == null) continue;

            String candidateCanonical = candidate.getCanonicalKey() != null ? candidate.getCanonicalKey() : "";
            String candidateTitleNorm = normalize(candidate.getProductName());

            // Skip identical product
            if ((!candidateCanonical.isBlank() && seenKeys.contains(candidateCanonical)) ||
                (!candidateTitleNorm.isBlank() && seenKeys.contains(candidateTitleNorm))) {
                continue;
            }

            // Category match check
            String candCat = candidate.getCategory() != null ? candidate.getCategory().toLowerCase() : "";
            if (!baseCat.isBlank() && !candCat.isBlank() && !baseCat.equals(candCat) &&
                !baseCat.contains(candCat) && !candCat.contains(baseCat)) {
                // If categories are completely different (e.g. Headphones vs Laptops), skip
                continue;
            }

            // Mark seen to prevent duplicate alternatives
            if (!candidateCanonical.isBlank()) seenKeys.add(candidateCanonical);
            if (!candidateTitleNorm.isBlank()) seenKeys.add(candidateTitleNorm);

            // Compute structured feature comparison
            FeatureComparisonDto comparison = featureComparisonService.compareFeatures(baseOffer, candidate);

            // Classify category type
            AlternativeCategoryType categoryType = classifyAlternative(baseOffer, candidate, comparison);
            String categoryLabel = getCategoryLabel(categoryType);

            // Compute similarity score (0-100)
            int similarityScore = computeAlternativeScore(baseOffer, candidate, comparison);

            // Generate verified highlights
            List<String> highlights = featureComparisonService.generateHighlights(
                    baseOffer, candidate, comparison, categoryType);

            candidateList.add(ProductAlternativeDto.builder()
                    .id(UUID.randomUUID().toString().substring(0, 8))
                    .productName(candidate.getProductName())
                    .brand(candidate.getBrand())
                    .category(candidate.getCategory())
                    .price(candidate.getPrice())
                    .originalPrice(candidate.getOriginalPrice())
                    .priceDifference(comparison.getPriceDiff())
                    .priceDifferencePercent(comparison.getPriceDiffPercent())
                    .imageUrl(candidate.getImageUrl())
                    .rating(candidate.getRating())
                    .merchant(candidate.getMerchant())
                    .productUrl(candidate.getProductUrl())
                    .categoryType(categoryType)
                    .categoryLabel(categoryLabel)
                    .similarityScore(similarityScore)
                    .highlights(highlights)
                    .featureComparison(comparison)
                    .attributes(candidate.getAttributes())
                    .build());
        }

        // Sort candidates by best diversity and score
        candidateList.sort(Comparator.comparing(ProductAlternativeDto::getSimilarityScore).reversed());

        return candidateList.stream().limit(maxResults).collect(Collectors.toList());
    }

    private AlternativeCategoryType classifyAlternative(
            NormalizedProductOfferDto base,
            NormalizedProductOfferDto alt,
            FeatureComparisonDto comp) {

        double diffPct = comp.getPriceDiffPercent() != null ? comp.getPriceDiffPercent() : 0.0;
        int betterSpecs = comp.getBetterSpecsCount() != null ? comp.getBetterSpecsCount() : 0;
        double altRating = alt.getRating() != null ? alt.getRating() : 4.5;
        double baseRating = base.getRating() != null ? base.getRating() : 4.5;

        if (diffPct <= -12.0) {
            return AlternativeCategoryType.CHEAPER_ALTERNATIVE;
        }

        if (diffPct <= -3.0 && (altRating >= 4.6 || betterSpecs >= 1)) {
            return AlternativeCategoryType.BETTER_VALUE;
        }

        if (Math.abs(diffPct) <= 12.0 && (betterSpecs >= 1 || altRating > baseRating)) {
            return AlternativeCategoryType.SIMILAR_PRICE_BETTER_FEATURE;
        }

        if (diffPct > 5.0 && (betterSpecs >= 1 || isProVariant(alt.getProductName()))) {
            return AlternativeCategoryType.PREMIUM_ALTERNATIVE;
        }

        if (diffPct < 0) {
            return AlternativeCategoryType.CHEAPER_ALTERNATIVE;
        }

        return AlternativeCategoryType.BETTER_VALUE;
    }

    private String getCategoryLabel(AlternativeCategoryType type) {
        if (type == null) return "Alternative Pick";
        switch (type) {
            case CHEAPER_ALTERNATIVE:
                return "Cheaper Option";
            case BETTER_VALUE:
                return "Best Value Pick";
            case SIMILAR_PRICE_BETTER_FEATURE:
                return "Feature Upgrade";
            case PREMIUM_ALTERNATIVE:
                return "Premium Option";
            default:
                return "Alternative Pick";
        }
    }

    private int computeAlternativeScore(
            NormalizedProductOfferDto base,
            NormalizedProductOfferDto alt,
            FeatureComparisonDto comp) {

        int score = 40; // Base score for same category candidate

        // Brand match / synergy
        if (base.getBrand() != null && alt.getBrand() != null && base.getBrand().equalsIgnoreCase(alt.getBrand())) {
            score += 20;
        } else {
            score += 10;
        }

        // Price proximity (reasonable ratio within +-40%)
        double absPct = Math.abs(comp.getPriceDiffPercent() != null ? comp.getPriceDiffPercent() : 0.0);
        if (absPct <= 15.0) {
            score += 25;
        } else if (absPct <= 35.0) {
            score += 15;
        } else {
            score += 5;
        }

        // High user rating
        if (alt.getRating() != null && alt.getRating() >= 4.8) {
            score += 15;
        } else if (alt.getRating() != null && alt.getRating() >= 4.5) {
            score += 10;
        }

        return Math.min(100, Math.max(10, score));
    }

    private boolean isProVariant(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("pro") || lower.contains("ultra") || lower.contains("max") || lower.contains("plus");
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
