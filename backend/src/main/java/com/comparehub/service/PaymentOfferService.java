package com.comparehub.service;

import com.comparehub.dto.PaymentOfferDto;
import com.comparehub.dto.PaymentOfferSummaryDto;
import com.comparehub.dto.PaymentPreferenceDto;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentOfferService {

    /**
     * Evaluates payment offers and returns the optimal payment option and full comparison breakdown.
     */
    PaymentOfferSummaryDto evaluatePaymentOffers(
            BigDecimal basePrice,
            BigDecimal additionalFees,
            String merchant,
            String category,
            PaymentPreferenceDto userPreference);

    /**
     * Retrieves available payment offers for a given merchant and product price.
     */
    List<PaymentOfferDto> getAvailableOffers(String merchant, String category, BigDecimal basePrice);

    /**
     * Retrieves saved payment preferences for a user, or defaults if not found / guest.
     */
    PaymentPreferenceDto getUserPreferences(Long userId);

    /**
     * Persists payment preferences for an authenticated user (bank/card type only, NEVER sensitive data).
     */
    PaymentPreferenceDto saveUserPreferences(Long userId, PaymentPreferenceDto preferenceDto);
}
