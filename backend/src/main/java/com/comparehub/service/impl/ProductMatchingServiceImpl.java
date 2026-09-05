package com.comparehub.service.impl;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductSimilarityResultDto;
import com.comparehub.service.ProductAttributeExtractor;
import com.comparehub.service.ProductMatchingService;
import com.comparehub.service.ProductSimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMatchingServiceImpl implements ProductMatchingService {

    private final ProductAttributeExtractor attributeExtractor;
    private final ProductSimilarityService similarityService;

    @Override
    public List<CanonicalProductGroupDto> matchAndGroupOffers(List<NormalizedProductOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        List<CanonicalProductGroupDto> groups = new ArrayList<>();

        for (NormalizedProductOfferDto offer : offers) {
            ProductAttributesDto attr = offer.getAttributes();
            if (attr == null) {
                attr = attributeExtractor.extractAttributes(offer.getProductName(), offer.getBrand(), offer.getCategory());
                offer.setAttributes(attr);
                offer.setCanonicalKey(attr.getCanonicalKey());
            }

            // Attempt to match against existing canonical groups
            CanonicalProductGroupDto matchedGroup = null;
            int bestScore = 0;

            for (CanonicalProductGroupDto group : groups) {
                ProductSimilarityResultDto sim = similarityService.calculateSimilarity(
                        attr, group.getAttributes(), offer.getProductName(), group.getCanonicalTitle());

                if (sim.isCompatible() && sim.getScore() > bestScore) {
                    bestScore = sim.getScore();
                    matchedGroup = group;
                }
            }

            if (matchedGroup != null && bestScore >= 75) {
                matchedGroup.getOffers().add(offer);
                offer.setCanonicalKey(matchedGroup.getCanonicalKey());
            } else {
                // Create a new canonical product cluster
                CanonicalProductGroupDto newGroup = CanonicalProductGroupDto.builder()
                        .canonicalKey(attr.getCanonicalKey())
                        .canonicalTitle(generateCanonicalTitle(attr, offer.getProductName()))
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

    private String generateCanonicalTitle(ProductAttributesDto attr, String fallback) {
        if (attr == null || attr.getModel() == null) return fallback;

        StringBuilder sb = new StringBuilder();
        if (attr.getBrand() != null && !attr.getBrand().equalsIgnoreCase("Generic")) {
            sb.append(attr.getBrand()).append(" ");
        }
        sb.append(attr.getModel());
        if (attr.getVariant() != null && !attr.getModel().toLowerCase().contains(attr.getVariant().toLowerCase())) {
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
