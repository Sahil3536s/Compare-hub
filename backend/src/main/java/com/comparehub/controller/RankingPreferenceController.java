package com.comparehub.controller;

import com.comparehub.dto.UserRankingPreferenceDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.PersonalizedRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ranking/preferences")
@RequiredArgsConstructor
public class RankingPreferenceController {

    private final PersonalizedRankingService personalizedRankingService;

    @GetMapping
    public ResponseEntity<UserRankingPreferenceDto> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        UserRankingPreferenceDto preferences = personalizedRankingService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping
    public ResponseEntity<UserRankingPreferenceDto> savePreferences(
            @RequestBody UserRankingPreferenceDto preferences,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        UserRankingPreferenceDto saved = personalizedRankingService.saveUserPreferences(principal.getId(), preferences);
        return ResponseEntity.ok(saved);
    }
}
