package com.comparehub.service;

import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.dto.RankingSummaryDto;

import java.util.List;

public interface RideRankingService {

    List<NormalizedRideOfferDto> rankAndBadgeRides(
            List<NormalizedRideOfferDto> offers, String sortBy);

    RankingSummaryDto getRankingSummary(List<NormalizedRideOfferDto> rankedOffers);
}
