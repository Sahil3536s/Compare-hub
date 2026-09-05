package com.comparehub.service;

import com.comparehub.dto.GroupTransportMode;
import com.comparehub.dto.GroupTravelOptionDto;
import com.comparehub.dto.GroupTravelRequestDto;

import java.util.List;

public interface GroupTransportModeEvaluator {

    GroupTransportMode getSupportedMode();

    List<GroupTravelOptionDto> evaluate(GroupTravelRequestDto request);
}
