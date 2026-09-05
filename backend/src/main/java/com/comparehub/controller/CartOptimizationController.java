package com.comparehub.controller;

import com.comparehub.dto.CartOptimizationRequestDto;
import com.comparehub.dto.CartOptimizationResponseDto;
import com.comparehub.service.CartOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartOptimizationController {

    private final CartOptimizationService cartOptimizationService;

    @PostMapping("/optimize")
    public ResponseEntity<CartOptimizationResponseDto> optimizeCart(
            @Valid @RequestBody CartOptimizationRequestDto request) {
        log.info("Received cart optimization request for {} items with strategy: {}",
                request.getItems() != null ? request.getItems().size() : 0, request.getStrategy());
        CartOptimizationResponseDto response = cartOptimizationService.optimizeCart(request);
        return ResponseEntity.ok(response);
    }
}
