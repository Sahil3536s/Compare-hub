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
public class CartItemOfferDto {

    private String itemName;
    private String matchedProductName;
    private String merchant;
    @Builder.Default
    private Integer quantity = 1;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String productUrl;
    private String imageUrl;
    private String deliveryText;
    private Double rating;
}
