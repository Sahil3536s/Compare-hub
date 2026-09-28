package com.comparehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    @NotBlank(message = "Item name is required")
    private String name;

    @Builder.Default
    private Integer quantity = 1;

    private BigDecimal maxPrice;
}
