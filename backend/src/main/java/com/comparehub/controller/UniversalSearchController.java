package com.comparehub.controller;

import com.comparehub.dto.UniversalSearchRequestDto;
import com.comparehub.dto.UniversalSearchResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.UniversalSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class UniversalSearchController {

    private final UniversalSearchService universalSearchService;

    @PostMapping
    public ResponseEntity<UniversalSearchResponseDto> search(
            @Valid @RequestBody UniversalSearchRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal != null && request.getUserId() == null) {
            request.setUserId(principal.getId());
        }

        UniversalSearchResponseDto response = universalSearchService.executeUniversalSearch(request);
        return ResponseEntity.ok(response);
    }
}
