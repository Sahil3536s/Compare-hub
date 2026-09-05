package com.comparehub.controller;

import com.comparehub.dto.FlightSearchResponseDto;
import com.comparehub.dto.RideSearchResponseDto;
import com.comparehub.dto.SearchHistoryRequestDto;
import com.comparehub.dto.SearchHistoryResponseDto;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.SearchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<List<SearchHistoryResponseDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<SearchHistoryResponseDto> history = searchHistoryService.getUserSearchHistory(userPrincipal.getId());
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<SearchHistoryResponseDto> recordSearch(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SearchHistoryRequestDto request) {
        request.setUserId(userPrincipal.getId());
        SearchHistoryResponseDto saved = searchHistoryService.recordSearch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        searchHistoryService.clearUserSearchHistory(userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Search history cleared successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHistoryItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id) {
        searchHistoryService.deleteSearchHistoryItem(userPrincipal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "History item deleted successfully"));
    }

    @GetMapping("/flights")
    public ResponseEntity<List<FlightSearchResponseDto>> getFlightSearches(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<FlightSearchResponseDto> flights = searchHistoryService.getUserFlightSearches(userPrincipal.getId());
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/rides")
    public ResponseEntity<List<RideSearchResponseDto>> getRideSearches(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<RideSearchResponseDto> rides = searchHistoryService.getUserRideSearches(userPrincipal.getId());
        return ResponseEntity.ok(rides);
    }
}
