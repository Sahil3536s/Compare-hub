package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.RankingSummaryDto;

import java.util.List;

public interface ProductRankingService {

    List<NormalizedProductOfferDto> rankAndMarkCheapest(
            List<NormalizedProductOfferDto> offers, String sortBy);

    RankingSummaryDto getRankingSummary(List<NormalizedProductOfferDto> rankedOffers);
}
