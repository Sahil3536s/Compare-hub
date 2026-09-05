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
public class GroupCostItemDto {

    private String label;
    private BigDecimal amount;
    private String category; // "FARE", "LOCAL_TRANSFER", "TAX_FEE", "DISCOUNT"
    private String description;
}
