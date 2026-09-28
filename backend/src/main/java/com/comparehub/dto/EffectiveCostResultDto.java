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
public class EffectiveCostResultDto {

    private BigDecimal basePrice;
    private BigDecimal deliveryCharge;
    private boolean deliveryChargeKnown;
    private BigDecimal mandatoryFee;
    private boolean mandatoryFeeKnown;
    private BigDecimal confirmedDiscount;
    private BigDecimal effectiveCost;
    private String currency;

    @Builder.Default
    private List<ConditionalOfferDto> conditionalOffers = new ArrayList<>();

    @Builder.Default
    private List<String> calculationNotes = new ArrayList<>();
}
