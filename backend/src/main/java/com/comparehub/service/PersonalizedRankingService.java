package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductRankingWeightsDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.dto.UserRankingPreferenceDto;

import java.util.List;

public interface PersonalizedRankingService {

    UserRankingPreferenceDto getUserPreferences(Long userId);

    UserRankingPreferenceDto saveUserPreferences(Long userId, UserRankingPreferenceDto preferences);

    List<NormalizedProductOfferDto> rankProductsWithCustomWeights(
            List<NormalizedProductOfferDto> offers, ProductRankingWeightsDto weights, String sortBy);

    RankingSummaryDto getPersonalizedRankingSummary(
            List<NormalizedProductOfferDto> rankedOffers, ProductRankingWeightsDto weights);
}
