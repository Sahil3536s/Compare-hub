package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.PricePointDto;
import com.comparehub.dto.PurchaseTimingDto;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseTimingService {

    PurchaseTimingDto analyzeTiming(BigDecimal currentPrice, List<PricePointDto> pricePoints);

    PurchaseTimingDto evaluateOfferTiming(NormalizedProductOfferDto offer);
}
