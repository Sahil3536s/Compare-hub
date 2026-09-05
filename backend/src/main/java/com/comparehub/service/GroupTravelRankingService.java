package com.comparehub.service;

import com.comparehub.dto.GroupTravelOptionDto;

import java.util.List;

public interface GroupTravelRankingService {

    List<GroupTravelOptionDto> rankOptions(List<GroupTravelOptionDto> options, String priority, int numberOfTravelers);
}
