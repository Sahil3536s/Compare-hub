package com.comparehub.service.impl;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductMatchResultDto;
import com.comparehub.service.ProductAttributeExtractor;
import com.comparehub.service.ProductMatchingService;
import com.comparehub.service.ProductSimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMatchingServiceImpl implements ProductMatchingService {

    private final ProductAttributeExtractor attributeExtractor;
    private final ProductSimilarityService similarityService;

    private static final double MATCH_THRESHOLD = 0.75;

    @Override
    public ProductMatchResultDto evaluateMatch(NormalizedProductOfferDto offerA, NormalizedProductOfferDto offerB) {
        if (offerA == null || offerB == null) {
            return ProductMatchResultDto.builder()
                    .matched(false)
                    .score(0.0)
                    .reasons(List.of("Null offer provided"))
                    .build();
        }

        ProductAttributesDto a = offerA.getAttributes() != null ? offerA.getAttributes()
                : attributeExtractor.extractAttributes(offerA);
        ProductAttributesDto b = offerB.getAttributes() != null ? offerB.getAttributes()
                : attributeExtractor.extractAttributes(offerB);

        List<String> positiveReasons = new ArrayList<>();
        List<String> rejectionReasons = new ArrayList<>();
        boolean isHardConflict = false;

        // 1. Brand matching & conflict detection
        if (a.getBrand() != null && b.getBrand() != null &&
                !a.getBrand().equalsIgnoreCase("Generic") && !b.getBrand().equalsIgnoreCase("Generic")) {
            if (a.getBrand().equalsIgnoreCase(b.getBrand())) {
                positiveReasons.add("Same brand");
            } else {
                isHardConflict = true;
                rejectionReasons.add("Brand mismatch: " + a.getBrand() + " vs " + b.getBrand());
            }
        }

        // 2. Model Number conflict detection
        if (a.getModelNumber() != null && b.getModelNumber() != null) {
            if (a.getModelNumber().equalsIgnoreCase(b.getModelNumber())) {
                positiveReasons.add("Same model number (" + a.getModelNumber() + ")");
            } else {
                isHardConflict = true;
                rejectionReasons.add("Model number mismatch: " + a.getModelNumber() + " vs " + b.getModelNumber());
            }
        }

        // 3. Storage variant conflict detection (Strict Variant Safety)
        if (a.getStorage() != null && b.getStorage() != null) {
            if (a.getStorage().equalsIgnoreCase(b.getStorage())) {
                positiveReasons.add("Same storage");
            } else {
                isHardConflict = true;
                rejectionReasons.add("Storage mismatch: " + a.getStorage() + " vs " + b.getStorage());
            }
        }

        // 4. RAM variant conflict detection (Strict Variant Safety)
        if (a.getRam() != null && b.getRam() != null) {
            if (a.getRam().equalsIgnoreCase(b.getRam())) {
                positiveReasons.add("Same RAM");
            } else {
                isHardConflict = true;
                rejectionReasons.add("RAM mismatch: " + a.getRam() + " vs " + b.getRam());
            }
        }

        // 5. Model & Variant conflict detection (e.g. S24 vs S24+, iPhone 16 vs iPhone 16 Pro)
        String modA = a.getModel() != null ? a.getModel().toLowerCase(Locale.ROOT) : "";
        String modB = b.getModel() != null ? b.getModel().toLowerCase(Locale.ROOT) : "";

        boolean aHasPlus = modA.contains("+") || modA.contains("plus") || "plus".equalsIgnoreCase(a.getVariant());
        boolean bHasPlus = modB.contains("+") || modB.contains("plus") || "plus".equalsIgnoreCase(b.getVariant());

        if (aHasPlus != bHasPlus) {
            isHardConflict = true;
            rejectionReasons.add("Model mismatch: S24 vs S24+");
        } else {
            String normA = normalizeModelTokens(modA);
            String normB = normalizeModelTokens(modB);
            if (!normA.isEmpty() && !normB.isEmpty()) {
                if (normA.equals(normB) || modA.equalsIgnoreCase(modB)) {
                    positiveReasons.add("Same model");
                } else if (normA.contains(normB) || normB.contains(normA)) {
                    positiveReasons.add("Same model family");
                } else {
                    isHardConflict = true;
                    rejectionReasons.add("Different models: " + a.getModel() + " vs " + b.getModel());
                }
            }
        }

        // Check explicit hardware variants (Pro, Pro Max, Ultra, Mini)
        if (isConflictingVariant(a.getVariant(), b.getVariant())) {
            isHardConflict = true;
            String vA = a.getVariant() != null ? a.getVariant() : "Base";
            String vB = b.getVariant() != null ? b.getVariant() : "Base";
            rejectionReasons.add("Variant mismatch: " + vA + " vs " + vB);
        } else if (a.getVariant() != null && b.getVariant() != null && a.getVariant().equalsIgnoreCase(b.getVariant())) {
            positiveReasons.add("Same variant (" + a.getVariant() + ")");
        }

        // 6. Color match
        if (a.getColor() != null && b.getColor() != null && a.getColor().equalsIgnoreCase(b.getColor())) {
            positiveReasons.add("Same color");
        }

        // 7. Calculate Deterministic Weighted Score (0.0 to 1.0)
        double score = 0.0;

        // Brand component (0.25)
        if (a.getBrand() != null && b.getBrand() != null && a.getBrand().equalsIgnoreCase(b.getBrand())) {
            score += 0.25;
        } else if (a.getBrand() == null || b.getBrand() == null || "Generic".equalsIgnoreCase(a.getBrand()) || "Generic".equalsIgnoreCase(b.getBrand())) {
            score += 0.15;
        }

        // Model component (0.35)
        if (positiveReasons.contains("Same model")) {
            score += 0.35;
        } else if (positiveReasons.contains("Same model family")) {
            score += 0.25;
        }

        // Variant component (0.15)
        if (a.getVariant() != null && b.getVariant() != null && a.getVariant().equalsIgnoreCase(b.getVariant())) {
            score += 0.15;
        } else if (a.getVariant() == null && b.getVariant() == null) {
            score += 0.15;
        } else if (!isHardConflict) {
            score += 0.08;
        }

        // Storage component (0.12)
        if (a.getStorage() != null && b.getStorage() != null && a.getStorage().equalsIgnoreCase(b.getStorage())) {
            score += 0.12;
        } else if (a.getStorage() == null || b.getStorage() == null) {
            score += 0.06;
        }

        // RAM component (0.08)
        if (a.getRam() != null && b.getRam() != null && a.getRam().equalsIgnoreCase(b.getRam())) {
            score += 0.08;
        } else if (a.getRam() == null || b.getRam() == null) {
            score += 0.04;
        }

        // Color component (0.05)
        if (a.getColor() != null && b.getColor() != null && a.getColor().equalsIgnoreCase(b.getColor())) {
            score += 0.05;
        } else {
            score += 0.03;
        }

        // Jaccard similarity bonus (up to +0.05)
        String titleA = offerA.getTitle() != null ? offerA.getTitle() : offerA.getProductName();
        String titleB = offerB.getTitle() != null ? offerB.getTitle() : offerB.getProductName();
        double jaccard = computeJaccardSimilarity(titleA, titleB);
        if (jaccard > 0.45) {
            score += 0.05;
        }

        // Model number match bonus (+0.10)
        if (a.getModelNumber() != null && b.getModelNumber() != null && a.getModelNumber().equalsIgnoreCase(b.getModelNumber())) {
            score += 0.10;
        }

        score = Math.min(1.0, Math.max(0.0, Math.round(score * 100.0) / 100.0));

        // 8. Final Decision
        boolean matched = !isHardConflict && score >= MATCH_THRESHOLD;

        List<String> combinedReasons = new ArrayList<>(positiveReasons);
        if (!rejectionReasons.isEmpty()) {
            combinedReasons.addAll(rejectionReasons);
        } else if (!matched) {
            combinedReasons.add("Score (" + score + ") below match threshold (" + MATCH_THRESHOLD + ")");
        }

        return ProductMatchResultDto.builder()
                .matched(matched)
                .score(score)
                .reasons(combinedReasons)
                .build();
    }

    @Override
    public List<CanonicalProductGroupDto> matchAndGroupOffers(List<NormalizedProductOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        List<CanonicalProductGroupDto> groups = new ArrayList<>();

        for (NormalizedProductOfferDto offer : offers) {
            if (offer.getAttributes() == null) {
                attributeExtractor.extractAttributes(offer);
            }

            CanonicalProductGroupDto matchedGroup = null;
            double bestScore = 0.0;

            for (CanonicalProductGroupDto group : groups) {
                NormalizedProductOfferDto representative = group.getOffers().get(0);
                ProductMatchResultDto matchResult = evaluateMatch(offer, representative);

                if (matchResult.isMatched() && matchResult.getScore() > bestScore) {
                    bestScore = matchResult.getScore();
                    matchedGroup = group;
                }
            }

            if (matchedGroup != null) {
                matchedGroup.getOffers().add(offer);
                offer.setCanonicalKey(matchedGroup.getCanonicalKey());
            } else {
                ProductAttributesDto attr = offer.getAttributes();
                CanonicalProductGroupDto newGroup = CanonicalProductGroupDto.builder()
                        .canonicalKey(attr.getCanonicalKey())
                        .canonicalTitle(generateCanonicalTitle(attr, offer.getTitle()))
                        .attributes(attr)
                        .offers(new ArrayList<>(List.of(offer)))
                        .build();
                groups.add(newGroup);
                offer.setCanonicalKey(newGroup.getCanonicalKey());
            }
        }

        // Calculate pricing metrics for each canonical group
        for (CanonicalProductGroupDto group : groups) {
            List<NormalizedProductOfferDto> groupOffers = group.getOffers();
            if (!groupOffers.isEmpty()) {
                NormalizedProductOfferDto lowest = groupOffers.stream()
                        .min(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                        .orElse(groupOffers.get(0));

                NormalizedProductOfferDto highest = groupOffers.stream()
                        .max(Comparator.comparing(NormalizedProductOfferDto::getPrice))
                        .orElse(groupOffers.get(0));

                group.setLowestPrice(lowest.getPrice());
                group.setHighestPrice(highest.getPrice());
                group.setCheapestMerchant(lowest.getMerchant());
                group.setPriceSpread(highest.getPrice().subtract(lowest.getPrice()));

                // Tag cheapest offer within matched variants
                for (NormalizedProductOfferDto off : groupOffers) {
                    if (off.getPrice().compareTo(lowest.getPrice()) == 0) {
                        off.setIsCheapest(true);
                    }
                }
            }
        }

        log.info("Matched {} raw multi-merchant offers into {} canonical product groups",
                offers.size(), groups.size());

        return groups;
    }

    @Override
    public List<NormalizedProductOfferDto> enrichWithMatching(List<NormalizedProductOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return offers;
        }

        matchAndGroupOffers(offers);
        return offers;
    }

    private boolean isConflictingVariant(String vA, String vB) {
        if (vA == null && vB == null) return false;

        String a = vA != null ? vA.toLowerCase(Locale.ROOT) : "";
        String b = vB != null ? vB.toLowerCase(Locale.ROOT) : "";

        if (a.equals(b)) return false;

        String[] majorVariants = { "pro max", "pro", "ultra", "plus", "mini", "max", "fe" };
        for (String mv : majorVariants) {
            boolean aHas = a.contains(mv);
            boolean bHas = b.contains(mv);
            if (aHas != bHas) {
                return true;
            }
        }

        return false;
    }

    private String normalizeModelTokens(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)\\bgalaxy\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
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

    private String generateCanonicalTitle(ProductAttributesDto attr, String fallback) {
        if (attr == null || attr.getModel() == null) return fallback;

        StringBuilder sb = new StringBuilder();
        if (attr.getBrand() != null && !attr.getBrand().equalsIgnoreCase("Generic")) {
            sb.append(attr.getBrand()).append(" ");
        }
        sb.append(attr.getModel());
        if (attr.getVariant() != null && !attr.getModel().toLowerCase(Locale.ROOT).contains(attr.getVariant().toLowerCase(Locale.ROOT))) {
            sb.append(" ").append(attr.getVariant());
        }
        if (attr.getRam() != null || attr.getStorage() != null || attr.getColor() != null) {
            sb.append(" (");
            List<String> specs = new ArrayList<>();
            if (attr.getRam() != null) specs.add(attr.getRam() + " RAM");
            if (attr.getStorage() != null) specs.add(attr.getStorage());
            if (attr.getColor() != null) specs.add(attr.getColor());
            sb.append(String.join(", ", specs));
            sb.append(")");
        }
        return sb.toString().trim();
    }
}
