package com.comparehub.service.impl;

import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductSimilarityResultDto;
import com.comparehub.service.ProductSimilarityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class ProductSimilarityServiceImpl implements ProductSimilarityService {

    @Value("${app.matching.strong-threshold:90}")
    private int strongThreshold = 90;

    @Value("${app.matching.probable-threshold:75}")
    private int probableThreshold = 75;

    @Override
    public ProductSimilarityResultDto calculateSimilarity(ProductAttributesDto a, ProductAttributesDto b, String titleA, String titleB) {
        if (a == null || b == null) {
            return ProductSimilarityResultDto.builder()
                    .score(0)
                    .matchQuality("SEPARATE_LISTING")
                    .isCompatible(false)
                    .explanation("Missing product attributes")
                    .build();
        }

        // =========================================================================
        // 1. HARD CONFLICT CHECKS (Zero Score if incompatible variants)
        // =========================================================================

        // Brand mismatch
        if (a.getBrand() != null && b.getBrand() != null &&
            !a.getBrand().equalsIgnoreCase("Generic") && !b.getBrand().equalsIgnoreCase("Generic") &&
            !a.getBrand().equalsIgnoreCase(b.getBrand())) {
            return ProductSimilarityResultDto.builder()
                    .score(0)
                    .matchQuality("SEPARATE_LISTING")
                    .isCompatible(false)
                    .explanation("Different brands: " + a.getBrand() + " vs " + b.getBrand())
                    .build();
        }

        // Storage mismatch
        if (a.getStorage() != null && b.getStorage() != null &&
            !a.getStorage().equalsIgnoreCase(b.getStorage())) {
            return ProductSimilarityResultDto.builder()
                    .score(0)
                    .matchQuality("SEPARATE_LISTING")
                    .isCompatible(false)
                    .explanation("Different storage capacities: " + a.getStorage() + " vs " + b.getStorage())
                    .build();
        }

        // RAM mismatch
        if (a.getRam() != null && b.getRam() != null &&
            !a.getRam().equalsIgnoreCase(b.getRam())) {
            return ProductSimilarityResultDto.builder()
                    .score(0)
                    .matchQuality("SEPARATE_LISTING")
                    .isCompatible(false)
                    .explanation("Different RAM sizes: " + a.getRam() + " vs " + b.getRam())
                    .build();
        }

        // Crucial Variant Conflict (e.g. Pro vs Pro Max, Ultra vs Plus, Standard vs Pro Max)
        if (isConflictingVariant(a.getVariant(), b.getVariant())) {
            return ProductSimilarityResultDto.builder()
                    .score(0)
                    .matchQuality("SEPARATE_LISTING")
                    .isCompatible(false)
                    .explanation("Conflicting product variants: " + a.getVariant() + " vs " + b.getVariant())
                    .build();
        }

        // =========================================================================
        // 2. WEIGHTED DETERMINISTIC ATTRIBUTE SCORING (Max 100)
        // =========================================================================
        int score = 0;

        // Brand match (25 pts)
        if (a.getBrand() != null && b.getBrand() != null && a.getBrand().equalsIgnoreCase(b.getBrand())) {
            score += 25;
        } else if (a.getBrand() == null || b.getBrand() == null) {
            score += 15;
        }

        // Model match (35 pts)
        if (a.getModel() != null && b.getModel() != null) {
            if (a.getModel().equalsIgnoreCase(b.getModel())) {
                score += 35;
            } else if (normalizeTokens(a.getModel()).equals(normalizeTokens(b.getModel()))) {
                score += 35;
            } else if (a.getModel().toLowerCase().contains(b.getModel().toLowerCase()) ||
                       b.getModel().toLowerCase().contains(a.getModel().toLowerCase())) {
                score += 25;
            }
        }

        // Variant match (15 pts)
        if (a.getVariant() != null && b.getVariant() != null) {
            if (a.getVariant().equalsIgnoreCase(b.getVariant())) {
                score += 15;
            }
        } else if (a.getVariant() == null && b.getVariant() == null) {
            score += 15; // Both standard variants
        } else {
            score += 8; // One has explicit minor variant (e.g. 5G) while other omits it
        }

        // RAM match (10 pts)
        if (a.getRam() != null && b.getRam() != null) {
            if (a.getRam().equalsIgnoreCase(b.getRam())) {
                score += 10;
            }
        } else {
            score += 8;
        }

        // Storage match (10 pts)
        if (a.getStorage() != null && b.getStorage() != null) {
            if (a.getStorage().equalsIgnoreCase(b.getStorage())) {
                score += 10;
            }
        } else {
            score += 8;
        }

        // Color match (5 pts)
        if (a.getColor() != null && b.getColor() != null) {
            if (a.getColor().equalsIgnoreCase(b.getColor())) {
                score += 5;
            } else {
                score += 2;
            }
        } else {
            score += 5;
        }

        // Token Jaccard bonus/adjustment
        double jaccard = computeJaccardSimilarity(titleA, titleB);
        if (jaccard > 0.6 && score < 95) {
            score = Math.min(100, score + 5);
        }

        score = Math.min(100, Math.max(0, score));

        String quality;
        if (score >= strongThreshold) {
            quality = "STRONG_MATCH";
        } else if (score >= probableThreshold) {
            quality = "PROBABLE_MATCH";
        } else {
            quality = "SEPARATE_LISTING";
        }

        return ProductSimilarityResultDto.builder()
                .score(score)
                .matchQuality(quality)
                .isCompatible(score >= probableThreshold)
                .explanation("Calculated similarity score: " + score + "/100 (" + quality + ")")
                .build();
    }

    private boolean isConflictingVariant(String vA, String vB) {
        if (vA == null && vB == null) return false;

        String a = vA != null ? vA.toLowerCase(Locale.ROOT) : "";
        String b = vB != null ? vB.toLowerCase(Locale.ROOT) : "";

        if (a.equals(b)) return false;

        // Major hardware variants that alter product identity significantly
        String[] majorVariants = { "pro max", "pro", "ultra", "plus", "mini", "max" };
        for (String mv : majorVariants) {
            boolean aHas = a.contains(mv);
            boolean bHas = b.contains(mv);
            if (aHas != bHas) {
                return true;
            }
        }

        return false;
    }

    private String normalizeTokens(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double computeJaccardSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        Set<String> set1 = new HashSet<>(List.of(s1.toLowerCase(Locale.ROOT).split("\\s+")));
        Set<String> set2 = new HashSet<>(List.of(s2.toLowerCase(Locale.ROOT).split("\\s+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
}
