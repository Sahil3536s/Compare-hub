package com.comparehub.service;

import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.ProductSearchRequestDto;

import java.util.List;

public interface ProductSearchOrchestrator {

    ProductComparisonResponseDto search(ProductSearchRequestDto request);

    List<String> getSearchSuggestions(String prefix);
}
