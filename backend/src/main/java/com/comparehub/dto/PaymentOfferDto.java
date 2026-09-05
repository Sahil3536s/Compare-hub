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
public class PaymentOfferDto {

    private String id;
    private String bank; // e.g. "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "ALL"
    private PaymentOfferType paymentType; // CREDIT_CARD, DEBIT_CARD, UPI, WALLET, COUPON
    private String title;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal minPurchaseAmount;
    private BigDecimal effectivePrice; // Price if this offer is used
    private PaymentEligibilityStatus eligibilityStatus; // ELIGIBLE, POSSIBLY_ELIGIBLE, NOT_ELIGIBLE, UNKNOWN
    private String eligibilityReason;
    @Builder.Default
    private Boolean isConditional = true;
    private String terms;
}
