package com.comparehub.service;

import com.comparehub.dto.CartOptimizationRequestDto;
import com.comparehub.dto.CartOptimizationResponseDto;

public interface CartOptimizationService {

    CartOptimizationResponseDto optimizeCart(CartOptimizationRequestDto request);
}
