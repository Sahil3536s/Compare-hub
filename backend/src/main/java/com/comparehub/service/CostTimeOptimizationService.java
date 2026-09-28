package com.comparehub.service;

import com.comparehub.dto.*;

import java.util.List;

public interface CostTimeOptimizationService {

    CostTimeOptimizationResponseDto optimize(CostTimeOptimizationRequestDto request);

    List<CostTimeOptionDto> rankOptions(List<CostTimeOptionDto> options, double costWeight);

    List<NormalizedFlightOfferDto> optimizeFlights(List<NormalizedFlightOfferDto> flights, double costWeight);

    List<NormalizedRideOfferDto> optimizeRides(List<NormalizedRideOfferDto> rides, double costWeight);

    List<GroupTravelOptionDto> optimizeGroupTravel(List<GroupTravelOptionDto> groupOptions, double costWeight);

    String generateTradeoffInsight(CostTimeOptionDto cheapest, CostTimeOptionDto fastest, CostTimeOptionDto top, double costWeight);
}
