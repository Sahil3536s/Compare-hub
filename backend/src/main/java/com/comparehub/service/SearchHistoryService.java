package com.comparehub.service;

import com.comparehub.dto.*;

import java.util.List;

public interface SearchHistoryService {

    SearchHistoryResponseDto recordSearch(SearchHistoryRequestDto request);

    List<SearchHistoryResponseDto> getUserSearchHistory(Long userId);

    void clearUserSearchHistory(Long userId);

    void deleteSearchHistoryItem(Long userId, Long historyId);

    FlightSearchResponseDto recordFlightSearch(FlightSearchRequestDto request);

    List<FlightSearchResponseDto> getUserFlightSearches(Long userId);

    RideSearchResponseDto recordRideSearch(RideSearchRequestDto request);

    List<RideSearchResponseDto> getUserRideSearches(Long userId);
}
