package com.comparehub.service;

import com.comparehub.dto.AlternativeCategoryType;
import com.comparehub.dto.FeatureComparisonDto;
import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface ProductFeatureComparisonService {

    /**
     * Computes deterministic structured specification comparison between two products.
     */
    FeatureComparisonDto compareFeatures(NormalizedProductOfferDto baseProduct, NormalizedProductOfferDto alternativeProduct);

    /**
     * Generates truthful, evidence-based highlight bullets based solely on structured differences.
     */
    List<String> generateHighlights(
            NormalizedProductOfferDto baseProduct,
            NormalizedProductOfferDto alternativeProduct,
            FeatureComparisonDto comparison,
            AlternativeCategoryType categoryType);
}
