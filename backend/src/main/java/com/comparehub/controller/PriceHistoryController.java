package com.comparehub.controller;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing price‑history data for a product.
 *
 * The backend already records price snapshots via {@code PriceHistoryServiceImpl}.
 * This controller simply forwards the request to the service and returns the DTO.
 * No fabrication of data occurs – if the service finds no history it returns a
 * placeholder response with {@code analysisText = "Price history is not available yet."}.
 */
@RestController
@RequestMapping("/api/price-history")
@RequiredArgsConstructor
@Slf4j
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    /**
     * Retrieve price‑history for a given product.
     *
     * @param productId the product identifier
     * @param period    optional period string ("7D", "30D", "90D"); defaults to "30D"
     * @return a {@link ProductPriceHistoryResponseDto} containing price points and statistics
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductPriceHistoryResponseDto> getHistory(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "30D") String period) {
        ProductPriceHistoryResponseDto response = priceHistoryService.getPriceHistory(productId, period);
        return ResponseEntity.ok(response);
    }
}
