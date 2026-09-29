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

    @GetMapping("/{productId}/price-meter")
    public ResponseEntity<com.comparehub.dto.PriceMeterDto> getPriceMeter(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "period", required = false, defaultValue = "30D") String period,
            @RequestParam(name = "currentPrice", required = false) java.math.BigDecimal currentPrice) {
        com.comparehub.dto.PriceMeterDto meter = priceHistoryService.calculatePriceMeter(productId, currentPrice, period);
        return ResponseEntity.ok(meter);
    }
}
