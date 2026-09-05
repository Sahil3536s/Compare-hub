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
public class CanonicalProductGroupDto {

    private String canonicalKey;
    private String canonicalTitle;
    private ProductAttributesDto attributes;

    @Builder.Default
    private List<NormalizedProductOfferDto> offers = new ArrayList<>();

    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private String cheapestMerchant;
    private BigDecimal priceSpread; // highestPrice - lowestPrice
}
