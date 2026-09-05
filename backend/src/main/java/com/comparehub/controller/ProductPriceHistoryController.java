package com.comparehub.controller;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductPriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("/{productId}/price-history")
    public ResponseEntity<ProductPriceHistoryResponseDto> getPriceHistory(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "period", required = false, defaultValue = "30D") String period) {
        ProductPriceHistoryResponseDto history = priceHistoryService.getPriceHistory(productId, period);
        return ResponseEntity.ok(history);
    }
}
