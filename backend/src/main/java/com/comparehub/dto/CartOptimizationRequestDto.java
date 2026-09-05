package com.comparehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartOptimizationRequestDto {

    @NotEmpty(message = "Cart must contain at least one item")
    @Valid
    @Builder.Default
    private List<CartItemDto> items = new ArrayList<>();

    @Builder.Default
    private String strategy = "MINIMIZE_PRICE"; // "MINIMIZE_PRICE" or "MINIMIZE_DELIVERIES"
}
