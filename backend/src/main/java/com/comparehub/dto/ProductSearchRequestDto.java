package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequestDto {

    @Builder.Default
    private String query = "";

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer pageSize = 20;

    private String merchant;
    private String brand;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStockOnly;

    @Builder.Default
    private String sortBy = "best";

    private Double minRating;
    private String delivery;
    private String ram;
    private String storage;

    @Builder.Default
    private Map<String, String> attributeFilters = new LinkedHashMap<>();
}
