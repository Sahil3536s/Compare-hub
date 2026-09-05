package com.comparehub.service;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;

import java.util.List;

public interface SavedProductService {

    SavedProductResponseDto saveProduct(SavedProductRequestDto request);

    List<SavedProductResponseDto> getSavedProductsByUser(Long userId);

    void removeSavedProduct(Long userId, Long productId);

    void removeSavedProductById(Long userId, Long savedProductId);
}
