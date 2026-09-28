package com.comparehub.controller;

import com.comparehub.dto.AirportResultDto;
import com.comparehub.service.AirportSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportSearchController {

    private final AirportSearchService airportSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<AirportResultDto>> searchAirports(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "query", required = false) String query) {

        String searchTerm = (q != null && !q.isBlank()) ? q : (query != null ? query : "");
        List<AirportResultDto> results = airportSearchService.searchAirports(searchTerm);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<AirportResultDto>> suggestAirports(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "query", required = false) String query) {

        String searchTerm = (q != null && !q.isBlank()) ? q : (query != null ? query : "");
        List<AirportResultDto> results = airportSearchService.searchAirports(searchTerm);
        return ResponseEntity.ok(results);
    }
}
