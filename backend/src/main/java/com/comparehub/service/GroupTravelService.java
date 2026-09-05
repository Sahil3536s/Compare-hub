package com.comparehub.service;

import com.comparehub.dto.GroupTravelRequestDto;
import com.comparehub.dto.GroupTravelResponseDto;

public interface GroupTravelService {

    GroupTravelResponseDto optimizeGroupTravel(GroupTravelRequestDto request);
}
