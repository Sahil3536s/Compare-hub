package com.comparehub.controller;

import com.comparehub.dto.SmartDealsPageDto;
import com.comparehub.model.SmartDealCategory;
import com.comparehub.service.DealDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealDiscoveryController {

    private final DealDiscoveryService dealDiscoveryService;

    @GetMapping("/smart")
    public ResponseEntity<SmartDealsPageDto> getSmartDeals(
            @RequestParam(required = false, defaultValue = "ALL") SmartDealCategory category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "8") int size) {
        log.info("REST request for smart deals with category: {}, page: {}, size: {}", category, page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        SmartDealsPageDto smartDeals = dealDiscoveryService.getPersonalizedSmartDeals(category, pageable);
        return ResponseEntity.ok(smartDeals);
    }
}
