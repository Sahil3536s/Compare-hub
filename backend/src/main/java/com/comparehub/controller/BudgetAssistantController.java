package com.comparehub.controller;

import com.comparehub.dto.BudgetConstraint;
import com.comparehub.dto.BudgetOptimizationResponseDto;
import com.comparehub.service.BudgetOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetAssistantController {

    private final BudgetOptimizationService budgetOptimizationService;

    @PostMapping("/optimize")
    public ResponseEntity<BudgetOptimizationResponseDto> optimizeBudgetPlan(@RequestBody BudgetConstraint constraint) {
        log.info("REST request to optimize budget plan for {} travelers (Budget: ₹{}, Route: {} -> {})",
                constraint.getTravelers(), constraint.getMaxBudget(), constraint.getOrigin(), constraint.getDestination());
        BudgetOptimizationResponseDto response = budgetOptimizationService.optimizeBudgetPlan(constraint);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/parse-query")
    public ResponseEntity<BudgetConstraint> parseQuery(@RequestBody Map<String, String> payload) {
        String query = payload.getOrDefault("query", "");
        log.info("REST request to parse natural language budget query: '{}'", query);
        BudgetConstraint constraint = budgetOptimizationService.parseNaturalLanguageQuery(query);
        return ResponseEntity.ok(constraint);
    }

    @GetMapping("/sample")
    public ResponseEntity<BudgetOptimizationResponseDto> getSampleBudgetPlan() {
        log.info("REST request for sample budget assistant plan");
        BudgetOptimizationResponseDto response = budgetOptimizationService.getSampleBudgetPlan();
        return ResponseEntity.ok(response);
    }
}
