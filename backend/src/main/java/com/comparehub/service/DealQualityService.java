package com.comparehub.service;

import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.NormalizedProductOfferDto;

import java.math.BigDecimal;

public interface DealQualityService {

    DealQualityDto evaluateOfferDealQuality(NormalizedProductOfferDto offer);

    DealQualityDto calculateDealQuality(
            BigDecimal currentPrice,
            BigDecimal historicalAverage,
            BigDecimal thirtyDayLow,
            BigDecimal ninetyDayLow,
            BigDecimal advertisedOriginalPrice);
}
