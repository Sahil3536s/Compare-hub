package com.comparehub.controller;

import com.comparehub.dto.CanonicalProductDetailDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.dto.ProductSearchRequestDto;
import com.comparehub.service.ProductComparisonService;
import com.comparehub.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductComparisonController {

    private final ProductComparisonService productComparisonService;
    private final ProductService productService;

    private static final Set<String> RESERVED_PARAMS = Set.of(
            "q", "page", "pagesize", "merchant", "brand", "category",
            "minprice", "maxprice", "instock", "sortby", "minrating",
            "ram", "storage", "delivery"
    );

    @GetMapping("/search")
    public ResponseEntity<ProductComparisonResponseDto> searchProducts(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize,
            @RequestParam(name = "merchant", required = false) String merchant,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "inStock", required = false) Boolean inStock,
            @RequestParam(name = "sortBy", required = false, defaultValue = "best") String sortBy,
            @RequestParam(name = "minRating", required = false) Double minRating,
            @RequestParam(name = "ram", required = false) String ram,
            @RequestParam(name = "storage", required = false) String storage,
            @RequestParam(name = "delivery", required = false) String delivery,
            @RequestParam Map<String, String> allParams) {

        // Security: Sanitize query & clamp limits
        String sanitizedQuery = sanitizeQuery(query);
        int validPage = Math.max(1, page);
        int validPageSize = Math.max(1, Math.min(pageSize, 100));

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            minPrice = BigDecimal.ZERO;
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            maxPrice = null;
        }

        // Collect arbitrary dynamic attribute filters
        Map<String, String> attributeFilters = new LinkedHashMap<>();
        if (allParams != null) {
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (key != null && val != null && !val.isBlank() && !RESERVED_PARAMS.contains(key.toLowerCase(Locale.ROOT))) {
                    attributeFilters.put(key, val.trim());
                }
            }
        }

        ProductSearchRequestDto request = ProductSearchRequestDto.builder()
                .query(sanitizedQuery)
                .page(validPage)
                .pageSize(validPageSize)
                .merchant(merchant)
                .brand(brand)
                .category(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStockOnly(inStock)
                .sortBy(sortBy)
                .minRating(minRating)
                .ram(ram)
                .storage(storage)
                .delivery(delivery)
                .attributeFilters(attributeFilters)
                .build();

        ProductComparisonResponseDto result = productComparisonService.search(request);
        if (result == null) {
            result = (minRating != null || ram != null || storage != null || delivery != null)
                    ? productComparisonService.compareProducts(
                            sanitizedQuery, merchant, brand, category, minPrice, maxPrice, inStock, sortBy, minRating, ram, storage, delivery)
                    : productComparisonService.compareProducts(
                            sanitizedQuery, merchant, brand, category, minPrice, maxPrice, inStock, sortBy);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanonicalProductDetailDto> getProductDetail(@PathVariable("id") Long id) {
        log.info("Fetching canonical product details for product id: {}", id);
        CanonicalProductDetailDto detail = productService.getCanonicalProductDetail(id);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        String sanitized = sanitizeQuery(query);
        if (sanitized.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<String> suggestions = productComparisonService.getSearchSuggestions(sanitized);
        return ResponseEntity.ok(suggestions);
    }

    private String sanitizeQuery(String q) {
        if (q == null) return "";
        // Strip HTML tags and control chars, trim, enforce max length 150
        String sanitized = q.replaceAll("<[^>]*>", "").replaceAll("[\\p{Cntrl}]", " ").trim();
        if (sanitized.length() > 150) {
            sanitized = sanitized.substring(0, 150).trim();
        }
        return sanitized;
    }
}
