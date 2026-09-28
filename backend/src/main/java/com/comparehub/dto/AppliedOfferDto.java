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
public class AppliedOfferDto {

    private String type; // "PAYMENT_OFFER", "COUPON", "INSTANT_DISCOUNT", "CASHBACK"
    private String description;
    private BigDecimal discountAmount;
    @Builder.Default
    private Boolean isConditional = false;
    private String terms;
}
