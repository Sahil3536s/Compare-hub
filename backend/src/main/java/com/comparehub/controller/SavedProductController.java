package com.comparehub.controller;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.SavedProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved/products")
@RequiredArgsConstructor
public class SavedProductController {

    private final SavedProductService savedProductService;

    @PostMapping
    public ResponseEntity<SavedProductResponseDto> saveProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SavedProductRequestDto request) {
        request.setUserId(userPrincipal.getId());
        SavedProductResponseDto saved = savedProductService.saveProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<SavedProductResponseDto>> getSavedProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<SavedProductResponseDto> savedList = savedProductService.getSavedProductsByUser(userPrincipal.getId());
        return ResponseEntity.ok(savedList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeSavedProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id) {
        savedProductService.removeSavedProductById(userPrincipal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Product removed from saved items successfully"));
    }
}
