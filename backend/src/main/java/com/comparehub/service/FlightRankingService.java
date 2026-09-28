package com.comparehub.service;

import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.RankingSummaryDto;

import java.util.List;

public interface FlightRankingService {

    List<NormalizedFlightOfferDto> rankAndBadgeFlights(
            List<NormalizedFlightOfferDto> offers, String sortBy);

    RankingSummaryDto getRankingSummary(List<NormalizedFlightOfferDto> rankedOffers);
}
