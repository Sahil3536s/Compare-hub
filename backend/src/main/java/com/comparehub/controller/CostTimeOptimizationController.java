package com.comparehub.controller;

import com.comparehub.dto.CostTimeOptimizationRequestDto;
import com.comparehub.dto.CostTimeOptimizationResponseDto;
import com.comparehub.service.CostTimeOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/optimize/cost-time")
@RequiredArgsConstructor
public class CostTimeOptimizationController {

    private final CostTimeOptimizationService optimizationService;

    @PostMapping
    public ResponseEntity<CostTimeOptimizationResponseDto> optimize(
            @Valid @RequestBody CostTimeOptimizationRequestDto request) {
        log.info("REST request for Cost vs Time optimization: {}% money / {}% time across {} options",
                request.getCostWeight(), request.getTimeWeight(),
                request.getOptions() != null ? request.getOptions().size() : 0);
        CostTimeOptimizationResponseDto response = optimizationService.optimize(request);
        return ResponseEntity.ok(response);
    }
}
