package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAlternativeDto;
import com.comparehub.dto.ProductAlternativesResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface ProductAlternativeService {

    /**
     * Discovers and ranks alternative product options for a target product query / parameters.
     */
    ProductAlternativesResponseDto getAlternatives(
            String productName,
            BigDecimal price,
            String category,
            String brand,
            int limit);

    /**
     * Evaluates a catalog against a base offer and produces categorized alternative recommendations.
     */
    List<ProductAlternativeDto> findAlternativesForOffer(
            NormalizedProductOfferDto baseOffer,
            List<NormalizedProductOfferDto> availableOffers,
            int limit);
}
