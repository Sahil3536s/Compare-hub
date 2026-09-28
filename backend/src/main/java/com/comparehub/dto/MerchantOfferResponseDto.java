package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOfferResponseDto {
    private Long id;
    private Long productId;
    private String merchant;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String productUrl;
    private Boolean inStock;
    private BigDecimal rating;
    private String deliveryText;
    private Instant lastUpdated;
}
