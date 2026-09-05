package com.comparehub.controller;

import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.service.ProductComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductComparisonController {

    private final ProductComparisonService productComparisonService;

    @GetMapping("/search")
    public ResponseEntity<ProductComparisonResponseDto> searchProducts(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(name = "merchant", required = false) String merchant,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "inStock", required = false) Boolean inStock,
            @RequestParam(name = "sortBy", required = false, defaultValue = "price_asc") String sortBy) {

        ProductComparisonResponseDto result = productComparisonService.compareProducts(
                query, merchant, brand, category, minPrice, maxPrice, inStock, sortBy);

        return ResponseEntity.ok(result);
    }
}
