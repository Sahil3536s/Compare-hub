package com.comparehub.controller;

import com.comparehub.dto.*;
import com.comparehub.service.DatabaseSeedService;
import com.comparehub.service.PriceAlertService;
import com.comparehub.service.ProductService;
import com.comparehub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller strictly for testing and validating database persistence,
 * relational mapping, Flyway migrations, and DTO layer in Phase 3.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class DatabaseTestController {

    private final UserService userService;
    private final ProductService productService;
    private final PriceAlertService priceAlertService;
    private final DatabaseSeedService databaseSeedService;

    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedSampleData() {
        String result = databaseSeedService.seedSampleData();
        return ResponseEntity.ok(Map.of("message", result));
    }

    // --- User Endpoints ---
    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // --- Product Endpoints ---
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(productService.getProductsByCategory(category));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping("/products/{id}/offers")
    public ResponseEntity<MerchantOfferResponseDto> addMerchantOffer(
            @PathVariable Long id,
            @Valid @RequestBody MerchantOfferRequestDto offerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addMerchantOffer(id, offerRequest));
    }

    // --- Price Alert Endpoints ---
    @GetMapping("/alerts")
    public ResponseEntity<List<PriceAlertResponseDto>> getActiveAlerts(
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return ResponseEntity.ok(priceAlertService.getAlertsByUserId(userId));
        }
        return ResponseEntity.ok(priceAlertService.getAllActiveAlerts());
    }

    @PostMapping("/alerts")
    public ResponseEntity<PriceAlertResponseDto> createPriceAlert(@Valid @RequestBody PriceAlertRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priceAlertService.createPriceAlert(request));
    }
}
