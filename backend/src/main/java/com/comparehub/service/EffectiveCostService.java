package com.comparehub.service;

import com.comparehub.dto.EffectiveCostResultDto;
import com.comparehub.dto.NormalizedProductOfferDto;

public interface EffectiveCostService {

    EffectiveCostResultDto calculateEffectiveCost(NormalizedProductOfferDto offer);
}
