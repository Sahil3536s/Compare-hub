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
public class ProductAlternativesResponseDto {

    private String baseProductName;
    private BigDecimal basePrice;
    private String baseCategory;
    private String baseBrand;
    private Integer totalAlternatives;

    @Builder.Default
    private List<ProductAlternativeDto> alternatives = new ArrayList<>();

    private String summary;
}
