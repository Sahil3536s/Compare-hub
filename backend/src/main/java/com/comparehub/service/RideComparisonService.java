package com.comparehub.service;

import com.comparehub.dto.RideCompareRequestDto;
import com.comparehub.dto.RideComparisonResponseDto;

public interface RideComparisonService {

    RideComparisonResponseDto compareRides(RideCompareRequestDto request);
}
