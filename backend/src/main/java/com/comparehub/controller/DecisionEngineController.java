package com.comparehub.controller;

import com.comparehub.dto.UnifiedDecisionRequest;
import com.comparehub.dto.UnifiedDecisionResponse;
import com.comparehub.service.DecisionEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/decision")
@RequiredArgsConstructor
public class DecisionEngineController {

    private final DecisionEngineService decisionEngineService;

    @PostMapping("/evaluate")
    public ResponseEntity<UnifiedDecisionResponse> evaluateDecision(@RequestBody UnifiedDecisionRequest request) {
        log.info("REST request to evaluate unified decision for query: '{}', type: '{}'",
                request != null ? request.getQuery() : "NULL",
                request != null ? request.getDecisionType() : "AUTO_DETECT");
        UnifiedDecisionResponse response = decisionEngineService.evaluate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sample/{type}")
    public ResponseEntity<UnifiedDecisionResponse> getSampleByType(@PathVariable String type) {
        log.info("REST request for sample decision of type: '{}'", type);
        UnifiedDecisionResponse response = decisionEngineService.evaluateSample(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sample")
    public ResponseEntity<UnifiedDecisionResponse> getDefaultSample() {
        return ResponseEntity.ok(decisionEngineService.evaluateSample("PRODUCT"));
    }
}
