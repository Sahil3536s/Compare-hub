package com.comparehub.service;

import com.comparehub.dto.BudgetConstraint;
import com.comparehub.dto.CartItemDto;
import com.comparehub.dto.UnifiedDecisionRequest;
import com.comparehub.dto.UnifiedDecisionResponse;

import java.util.List;

public interface DecisionEngineService {

    UnifiedDecisionResponse evaluate(UnifiedDecisionRequest request);

    UnifiedDecisionResponse evaluateSample(String decisionType);

    UnifiedDecisionResponse evaluateProduct(String query, Long userId, String priority);

    UnifiedDecisionResponse evaluateCart(List<CartItemDto> cartItems, Long userId);

    UnifiedDecisionResponse evaluateFlight(String origin, String destination, String date, String timePref, Integer stops, String priority);

    UnifiedDecisionResponse evaluateRide(Double pickupLat, Double pickupLng, Double dropoffLat, Double dropoffLng, String priority);

    UnifiedDecisionResponse evaluateJourney(String origin, String destination, String date, Integer travelers, String priority);

    UnifiedDecisionResponse evaluateBudget(BudgetConstraint constraint);

    UnifiedDecisionResponse evaluateGroupTravel(String origin, String destination, String date, Integer travelers, String priority);
}
