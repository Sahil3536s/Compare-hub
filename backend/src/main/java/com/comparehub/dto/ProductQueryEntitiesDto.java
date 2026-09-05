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
public class ProductQueryEntitiesDto {

    private String category; // e.g. "smartphones", "laptops", "headphones"
    private String brand; // e.g. "Samsung", "Apple", "Sony"
    private BigDecimal maxPrice; // e.g. 35000
    private BigDecimal minPrice;
    private String storage; // e.g. "256GB"
    private String ram; // e.g. "16GB"
    private String priority; // e.g. "camera", "battery", "gaming", "performance", "sound"
    private String sortPreference; // e.g. "price_asc", "rating_desc", "best_value"
}
