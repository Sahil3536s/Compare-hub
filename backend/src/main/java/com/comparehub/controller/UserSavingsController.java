package com.comparehub.controller;

import com.comparehub.dto.SavingsEventDto;
import com.comparehub.dto.SavingsSummaryDto;
import com.comparehub.service.UserSavingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
public class UserSavingsController {

    private final UserSavingsService userSavingsService;

    @GetMapping("/dashboard")
    public ResponseEntity<SavingsSummaryDto> getSavingsDashboard(
            @RequestParam(required = false) Long userId) {
        log.info("REST request to fetch personal savings dashboard for user ID: {}", userId);
        SavingsSummaryDto summary = userSavingsService.getSavingsSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/events")
    public ResponseEntity<SavingsEventDto> recordSavingsEvent(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody SavingsEventDto eventDto) {
        log.info("REST request to record savings event: '{}' (₹{})", eventDto.getTitle(), eventDto.getSavingAmount());
        SavingsEventDto saved = userSavingsService.recordSavingsEvent(userId, eventDto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/events/{id}/confirm")
    public ResponseEntity<SavingsEventDto> confirmSavingsEvent(
            @PathVariable Long id) {
        log.info("REST request to confirm savings event ID: {}", id);
        SavingsEventDto updated = userSavingsService.confirmSavings(id);
        return ResponseEntity.ok(updated);
    }
}
