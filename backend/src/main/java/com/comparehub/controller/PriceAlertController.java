package com.comparehub.controller;

import com.comparehub.dto.PriceAlertRequestDto;
import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.PriceAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<PriceAlertResponseDto> createAlert(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PriceAlertRequestDto request) {
        request.setUserId(userPrincipal.getId());
        PriceAlertResponseDto created = priceAlertService.createPriceAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PriceAlertResponseDto>> getAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<PriceAlertResponseDto> alerts = priceAlertService.getAlertsByUserId(userPrincipal.getId());
        return ResponseEntity.ok(alerts);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id,
            @RequestParam("active") boolean active) {
        priceAlertService.toggleAlertStatus(id, active);
        return ResponseEntity.ok(Map.of("id", id, "active", active, "message", "Alert status updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAlert(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id) {
        priceAlertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("message", "Alert deleted successfully"));
    }
}
