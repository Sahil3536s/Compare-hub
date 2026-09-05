package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPreferenceDto {

    private Long userId;

    @Builder.Default
    private String preferredBank = "ALL"; // e.g. "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "ALL"

    @Builder.Default
    private String preferredCardType = "ALL"; // "CREDIT_CARD", "DEBIT_CARD", "ALL"

    @Builder.Default
    private Boolean hasUpi = true;

    @Builder.Default
    private Boolean hasWallet = false;

    @Builder.Default
    private String preferredWallet = "NONE"; // "PAYTM", "AMAZON_PAY", "MOBIKWIK", "NONE"
}
