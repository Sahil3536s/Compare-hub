package com.comparehub.service;

import com.comparehub.dto.ProductComparisonResponseDto;

import java.math.BigDecimal;
import java.util.List;

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

    ProductComparisonResponseDto compareProducts(
            String query,
            String merchant,
            String brand,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy,
            Double minRating,
            String ram,
            String storage,
            String delivery);

    ProductComparisonResponseDto search(com.comparehub.dto.ProductSearchRequestDto request);

    List<String> getSearchSuggestions(String prefix);
}
