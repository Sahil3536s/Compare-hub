package com.comparehub.service;

import com.comparehub.dto.AppliedOfferDto;

import java.math.BigDecimal;
import java.util.List;

public interface OfferEligibilityService {

    List<AppliedOfferDto> evaluateOffers(String merchant, String category, BigDecimal basePrice);

    BigDecimal determineDeliveryFee(String merchant, String deliveryText, BigDecimal basePrice);

    BigDecimal determinePlatformFee(String merchant);
}
