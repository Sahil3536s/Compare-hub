package com.comparehub.controller;

import com.comparehub.dto.ProductAlternativesResponseDto;
import com.comparehub.service.ProductAlternativeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/products/alternatives")
@RequiredArgsConstructor
public class ProductAlternativeController {

    private final ProductAlternativeService productAlternativeService;

    @GetMapping
    public ResponseEntity<ProductAlternativesResponseDto> getAlternatives(
            @RequestParam(name = "productName", required = false, defaultValue = "Smartphone") String productName,
            @RequestParam(name = "price", required = false) BigDecimal price,
            @RequestParam(name = "category", required = false, defaultValue = "Smartphones") String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "limit", required = false, defaultValue = "4") int limit) {

        log.info("Fetching product alternatives for: '{}', price: {}, category: '{}'", productName, price, category);
        ProductAlternativesResponseDto response = productAlternativeService.getAlternatives(
                productName, price, category, brand, limit);

        return ResponseEntity.ok(response);
    }
}
