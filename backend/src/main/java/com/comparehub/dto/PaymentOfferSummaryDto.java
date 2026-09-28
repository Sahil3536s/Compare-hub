package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOfferSummaryDto {

    private BigDecimal standardPrice; // Base price + fees without payment discounts
    private BigDecimal bestEligiblePrice; // Lowest effective price among eligible/possibly eligible offers
    private BigDecimal maxSavings; // standardPrice - bestEligiblePrice
    private PaymentOfferDto bestOffer; // The top offer providing bestEligiblePrice
    
    @Builder.Default
    private List<PaymentOfferDto> offers = new ArrayList<>(); // All evaluated payment offers

    private PaymentPreferenceDto userPreference;

    @Builder.Default
    private String securityNote = "??? CompareHub never collects or stores card numbers, CVVs, PINs, or OTPs. Only bank preferences are used to find matching discounts.";
}
