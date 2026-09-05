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
public class CartMerchantOrderDto {

    private String merchant;
    @Builder.Default
    private List<CartItemOfferDto> items = new ArrayList<>();
    private BigDecimal itemsSubtotal;
    private BigDecimal deliveryFee;
    private BigDecimal platformFee;
    private BigDecimal merchantTotal;
}
