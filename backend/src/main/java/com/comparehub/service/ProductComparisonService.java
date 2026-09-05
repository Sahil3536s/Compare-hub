package com.comparehub.service;

import com.comparehub.dto.ProductComparisonResponseDto;

import java.math.BigDecimal;

public interface ProductComparisonService {

    ProductComparisonResponseDto compareProducts(
            String query,
            String merchant,
            String brand,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy);
}
