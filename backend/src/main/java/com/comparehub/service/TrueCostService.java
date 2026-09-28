package com.comparehub.service;

import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface TrueCostService {

    CostBreakdownDto calculateTrueCost(NormalizedProductOfferDto offer);

    List<NormalizedProductOfferDto> enrichWithTrueCost(List<NormalizedProductOfferDto> offers);
}
