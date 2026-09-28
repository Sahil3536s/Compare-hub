package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionalOfferDto {

    private String offerId;
    private String description;
    private BigDecimal discountAmount;

    @Builder.Default
    private boolean conditionalOffer = true;

    private String conditionType; // e.g. "BANK_CARD", "MIN_CART_VALUE", "MEMBERSHIP"
    private String requiredBankOrCard; // e.g. "HDFC Bank Credit Card", "SBI Credit Card"
    private BigDecimal minCartValue;
    private String terms;
}
