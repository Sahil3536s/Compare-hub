package com.comparehub.service;

import com.comparehub.dto.AiRecommendationDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.NormalizedRideOfferDto;

import java.util.List;

public interface ComparisonRecommendationService {

    AiRecommendationDto recommendProducts(List<NormalizedProductOfferDto> offers);

    AiRecommendationDto recommendFlights(List<NormalizedFlightOfferDto> offers);

    AiRecommendationDto recommendRides(List<NormalizedRideOfferDto> offers);
}
