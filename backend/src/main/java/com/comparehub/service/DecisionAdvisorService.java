package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.BudgetPreference;

import java.util.List;

public interface DecisionAdvisorService {

    DecisionRecommendation advise(DecisionContext context);

    DecisionRecommendation adviseProduct(List<NormalizedProductOfferDto> offers, String userPriority);

    DecisionRecommendation adviseFlight(List<NormalizedFlightOfferDto> offers, String userPriority);

    DecisionRecommendation adviseRide(List<NormalizedRideOfferDto> offers, String userPriority);

    DecisionRecommendation adviseJourney(List<JourneyOptionDto> options, String userPriority);

    DecisionRecommendation adviseGroupTravel(List<GroupTravelOptionDto> options, String userPriority);

    DecisionRecommendation adviseBudgetPlan(List<BudgetPlanOptionDto> plans, BudgetPreference preference);

    DecisionRecommendation getSampleProductAdvise();

    DecisionRecommendation getSampleTravelAdvise();
}
