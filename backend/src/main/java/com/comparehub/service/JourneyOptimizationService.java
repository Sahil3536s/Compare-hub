package com.comparehub.service;

import com.comparehub.dto.SmartJourneyRequestDto;
import com.comparehub.dto.SmartJourneyResponseDto;

public interface JourneyOptimizationService {

    SmartJourneyResponseDto optimizeJourney(SmartJourneyRequestDto request);

    SmartJourneyResponseDto getSampleJourney(String origin, String destination, int travelers, int bufferMinutes);
}
