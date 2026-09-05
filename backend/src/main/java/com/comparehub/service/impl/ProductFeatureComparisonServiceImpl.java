package com.comparehub.service.impl;

import com.comparehub.dto.AlternativeCategoryType;
import com.comparehub.dto.FeatureComparisonDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.service.ProductFeatureComparisonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProductFeatureComparisonServiceImpl implements ProductFeatureComparisonService {

    private static final Pattern GB_PATTERN = Pattern.compile("(\\d+)\\s*(?:gb|g|tb)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCREEN_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:inch|\"|cm)", Pattern.CASE_INSENSITIVE);

    @Override
    public FeatureComparisonDto compareFeatures(NormalizedProductOfferDto baseProduct, NormalizedProductOfferDto alternativeProduct) {
        if (baseProduct == null || alternativeProduct == null) {
            return null;
        }

        BigDecimal basePrice = baseProduct.getPrice() != null ? baseProduct.getPrice() : BigDecimal.ZERO;
        BigDecimal altPrice = alternativeProduct.getPrice() != null ? alternativeProduct.getPrice() : BigDecimal.ZERO;

        BigDecimal priceDiff = altPrice.subtract(basePrice);
        double priceDiffPercent = 0.0;
        if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
            priceDiffPercent = priceDiff.multiply(BigDecimal.valueOf(100))
                    .divide(basePrice, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        boolean isCheaper = priceDiff.compareTo(BigDecimal.ZERO) < 0;

        double baseRating = baseProduct.getRating() != null ? baseProduct.getRating() : 0.0;
        double altRating = alternativeProduct.getRating() != null ? alternativeProduct.getRating() : 0.0;
        double ratingDiff = Math.round((altRating - baseRating) * 10.0) / 10.0;

        ProductAttributesDto attrBase = baseProduct.getAttributes();
        ProductAttributesDto attrAlt = alternativeProduct.getAttributes();

        int betterSpecsCount = 0;
        String ramComparison = null;
        String storageComparison = null;
        String screenSizeComparison = null;
        String processorComparison = null;

        if (attrBase != null && attrAlt != null) {
            // RAM comparison
            if (attrAlt.getRam() != null && attrBase.getRam() != null) {
                int ramBase = parseGigabytes(attrBase.getRam());
                int ramAlt = parseGigabytes(attrAlt.getRam());
                if (ramAlt > ramBase && ramBase > 0) {
                    ramComparison = String.format("%s vs %s (+%dGB RAM)", attrAlt.getRam(), attrBase.getRam(), (ramAlt - ramBase));
                    betterSpecsCount++;
                } else if (ramAlt < ramBase) {
                    ramComparison = String.format("%s vs %s", attrAlt.getRam(), attrBase.getRam());
                } else {
                    ramComparison = String.format("Equal RAM (%s)", attrAlt.getRam());
                }
            }

            // Storage comparison
            if (attrAlt.getStorage() != null && attrBase.getStorage() != null) {
                int storageBase = parseGigabytes(attrBase.getStorage());
                int storageAlt = parseGigabytes(attrAlt.getStorage());
                if (storageAlt > storageBase && storageBase > 0) {
                    storageComparison = String.format("%s vs %s (+%dGB Storage)", attrAlt.getStorage(), attrBase.getStorage(), (storageAlt - storageBase));
                    betterSpecsCount++;
                } else if (storageAlt < storageBase) {
                    storageComparison = String.format("%s vs %s", attrAlt.getStorage(), attrBase.getStorage());
                } else {
                    storageComparison = String.format("Equal Storage (%s)", attrAlt.getStorage());
                }
            }

            // Screen size comparison
            if (attrAlt.getScreenSize() != null && attrBase.getScreenSize() != null) {
                double screenBase = parseScreenSize(attrBase.getScreenSize());
                double screenAlt = parseScreenSize(attrAlt.getScreenSize());
                if (screenAlt > screenBase && screenBase > 0) {
                    screenSizeComparison = String.format("%s vs %s (Larger Display)", attrAlt.getScreenSize(), attrBase.getScreenSize());
                    betterSpecsCount++;
                } else {
                    screenSizeComparison = String.format("%s vs %s", attrAlt.getScreenSize(), attrBase.getScreenSize());
                }
            }

            // Processor comparison
            if (attrAlt.getProcessor() != null && attrBase.getProcessor() != null) {
                if (!attrAlt.getProcessor().equalsIgnoreCase(attrBase.getProcessor())) {
                    processorComparison = String.format("%s vs %s", attrAlt.getProcessor(), attrBase.getProcessor());
                }
            }
        }

        if (ratingDiff > 0) {
            betterSpecsCount++;
        }

        return FeatureComparisonDto.builder()
                .priceDiff(priceDiff)
                .priceDiffPercent(priceDiffPercent)
                .isCheaper(isCheaper)
                .ratingDiff(ratingDiff)
                .ramComparison(ramComparison)
                .storageComparison(storageComparison)
                .screenSizeComparison(screenSizeComparison)
                .processorComparison(processorComparison)
                .betterSpecsCount(betterSpecsCount)
                .build();
    }

    @Override
    public List<String> generateHighlights(
            NormalizedProductOfferDto baseProduct,
            NormalizedProductOfferDto alternativeProduct,
            FeatureComparisonDto comparison,
            AlternativeCategoryType categoryType) {

        List<String> highlights = new ArrayList<>();
        if (comparison == null) return highlights;

        // 1. Price Highlight
        if (comparison.getPriceDiff() != null) {
            long absDiff = Math.abs(comparison.getPriceDiff().longValue());
            double absPercent = Math.abs(comparison.getPriceDiffPercent());

            if (Boolean.TRUE.equals(comparison.getIsCheaper())) {
                highlights.add(String.format("?%,d cheaper (%.1f%% lower price)", absDiff, absPercent));
            } else if (comparison.getPriceDiff().compareTo(BigDecimal.ZERO) > 0 && categoryType == AlternativeCategoryType.PREMIUM_ALTERNATIVE) {
                highlights.add(String.format("Flagship tier for +?%,d (%.1f%% extra)", absDiff, absPercent));
            } else if (comparison.getPriceDiff().compareTo(BigDecimal.ZERO) == 0) {
                highlights.add("Matched price point");
            }
        }

        // 2. Hardware Spec Highlights (RAM / Storage / Screen / Processor)
        if (comparison.getRamComparison() != null && comparison.getRamComparison().contains("+")) {
            highlights.add("More memory: " + comparison.getRamComparison());
        }
        if (comparison.getStorageComparison() != null && comparison.getStorageComparison().contains("+")) {
            highlights.add("Larger capacity: " + comparison.getStorageComparison());
        }
        if (comparison.getScreenSizeComparison() != null && comparison.getScreenSizeComparison().contains("Larger")) {
            highlights.add("Larger display: " + comparison.getScreenSizeComparison());
        }
        if (comparison.getProcessorComparison() != null) {
            highlights.add("Processor: " + comparison.getProcessorComparison());
        }

        // 3. Rating Delta Highlight
        if (comparison.getRatingDiff() != null && comparison.getRatingDiff() > 0.1) {
            highlights.add(String.format("Higher user rating: %.1f? vs %.1f? (+%.1f rating)",
                    alternativeProduct.getRating(), baseProduct.getRating(), comparison.getRatingDiff()));
        } else if (alternativeProduct.getRating() != null && alternativeProduct.getRating() >= 4.7) {
            highlights.add(String.format("High satisfaction rating: %.1f?", alternativeProduct.getRating()));
        }

        // 4. In-Stock / Merchant credibility
        if (Boolean.TRUE.equals(alternativeProduct.getInStock()) && Boolean.FALSE.equals(baseProduct.getInStock())) {
            highlights.add("Currently In Stock (Base product is Out of Stock)");
        }

        return highlights;
    }

    private int parseGigabytes(String s) {
        if (s == null) return 0;
        String lower = s.toLowerCase().trim();
        if (lower.contains("tb")) {
            Matcher m = Pattern.compile("(\\d+)").matcher(lower);
            if (m.find()) return Integer.parseInt(m.group(1)) * 1024;
        }
        Matcher m = GB_PATTERN.matcher(lower);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private double parseScreenSize(String s) {
        if (s == null) return 0.0;
        Matcher m = SCREEN_PATTERN.matcher(s.toLowerCase().trim());
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
}
