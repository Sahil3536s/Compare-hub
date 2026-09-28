package com.comparehub.controller;

import com.comparehub.dto.DecisionContext;
import com.comparehub.dto.DecisionRecommendation;
import com.comparehub.service.DecisionAdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
public class DecisionAdvisorController {

    private final DecisionAdvisorService decisionAdvisorService;

    @PostMapping("/evaluate")
    public ResponseEntity<DecisionRecommendation> evaluateDecision(@RequestBody DecisionContext context) {
        log.info("REST request to evaluate decision context of type: {} (priority: {})",
                context != null ? context.getContextType() : "NULL",
                context != null ? context.getUserPriority() : "DEFAULT");
        DecisionRecommendation recommendation = decisionAdvisorService.advise(context);
        return ResponseEntity.ok(recommendation);
    }

    @GetMapping("/sample/product")
    public ResponseEntity<DecisionRecommendation> getSampleProduct() {
        log.info("REST request for sample product decision recommendation");
        return ResponseEntity.ok(decisionAdvisorService.getSampleProductAdvise());
    }

    @GetMapping("/sample/travel")
    public ResponseEntity<DecisionRecommendation> getSampleTravel() {
        log.info("REST request for sample travel decision recommendation");
        return ResponseEntity.ok(decisionAdvisorService.getSampleTravelAdvise());
    }

    @GetMapping("/sample")
    public ResponseEntity<DecisionRecommendation> getSample() {
        return ResponseEntity.ok(decisionAdvisorService.getSampleProductAdvise());
    }
}
