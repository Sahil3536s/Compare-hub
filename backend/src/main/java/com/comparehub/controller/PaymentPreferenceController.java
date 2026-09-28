package com.comparehub.controller;

import com.comparehub.dto.PaymentPreferenceDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.PaymentOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/preferences/payment")
@RequiredArgsConstructor
public class PaymentPreferenceController {

    private final PaymentOfferService paymentOfferService;

    @GetMapping
    public ResponseEntity<PaymentPreferenceDto> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        PaymentPreferenceDto preferences = paymentOfferService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping
    public ResponseEntity<PaymentPreferenceDto> savePreferences(
            @RequestBody PaymentPreferenceDto preferences,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        PaymentPreferenceDto saved = paymentOfferService.saveUserPreferences(principal.getId(), preferences);
        return ResponseEntity.ok(saved);
    }
}
