package com.comparehub.service;

import com.comparehub.dto.FlightComparisonResponseDto;
import com.comparehub.dto.FlightSearchRequestDto;

public interface FlightComparisonService {

    FlightComparisonResponseDto compareFlights(FlightSearchRequestDto request);
}
