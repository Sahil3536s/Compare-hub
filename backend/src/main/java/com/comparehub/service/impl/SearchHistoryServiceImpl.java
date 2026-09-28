package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.*;
import com.comparehub.repository.FlightSearchRepository;
import com.comparehub.repository.RideSearchRepository;
import com.comparehub.repository.SearchHistoryRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final FlightSearchRepository flightSearchRepository;
    private final RideSearchRepository rideSearchRepository;
    private final UserRepository userRepository;

    private static final int MAX_HISTORY_PER_USER = 100;

    @Override
    @Transactional
    public SearchHistoryResponseDto recordSearch(SearchHistoryRequestDto request) {
        if (request.getQuery() == null || request.getQuery().trim().isBlank()) {
            return null; // Save only meaningful searches
        }

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

            // History growth control: keep table bounded per user
            long currentCount = searchHistoryRepository.countByUserId(user.getId());
            if (currentCount >= MAX_HISTORY_PER_USER) {
                List<SearchHistory> oldest = searchHistoryRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
                int toDelete = (int) (currentCount - MAX_HISTORY_PER_USER + 1);
                for (int i = 0; i < toDelete && i < oldest.size(); i++) {
                    searchHistoryRepository.delete(oldest.get(i));
                }
            }
        }

        SearchHistory history = SearchHistory.builder()
                .user(user)
                .query(request.getQuery().trim())
                .searchType(request.getSearchType())
                .details(request.getDetails())
                .targetUrl(request.getTargetUrl())
                .build();

        SearchHistory saved = searchHistoryRepository.save(history);
        return SearchHistoryResponseDto.builder()
                .id(saved.getId())
                .userId(saved.getUser() != null ? saved.getUser().getId() : null)
                .query(saved.getQuery())
                .searchType(saved.getSearchType())
                .details(saved.getDetails())
                .targetUrl(saved.getTargetUrl())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponseDto> getUserSearchHistory(Long userId) {
        return searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(h -> SearchHistoryResponseDto.builder()
                        .id(h.getId())
                        .userId(h.getUser() != null ? h.getUser().getId() : null)
                        .query(h.getQuery())
                        .searchType(h.getSearchType())
                        .details(h.getDetails())
                        .targetUrl(h.getTargetUrl())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearUserSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteSearchHistoryItem(Long userId, Long historyId) {
        searchHistoryRepository.deleteByIdAndUserId(historyId, userId);
    }

    @Override
    @Transactional
    public FlightSearchResponseDto recordFlightSearch(FlightSearchRequestDto request) {
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        LocalDate depDate = null;
        if (request.getDepartureDate() != null) {
            try {
                depDate = LocalDate.parse(request.getDepartureDate());
            } catch (Exception e) {
                depDate = LocalDate.now().plusDays(7);
            }
        } else {
            depDate = LocalDate.now().plusDays(7);
        }

        LocalDate retDate = null;
        if (request.getReturnDate() != null) {
            try {
                retDate = LocalDate.parse(request.getReturnDate());
            } catch (Exception ignored) {}
        }

        FlightSearch search = FlightSearch.builder()
                .user(user)
                .fromAirport(request.getFromAirport().trim().toUpperCase())
                .toAirport(request.getToAirport().trim().toUpperCase())
                .departureDate(depDate)
                .returnDate(retDate)
                .passengers(request.getPassengers())
                .cabinClass(request.getCabinClass())
                .build();

        FlightSearch saved = flightSearchRepository.save(search);
        return FlightSearchResponseDto.builder()
                .id(saved.getId())
                .userId(saved.getUser() != null ? saved.getUser().getId() : null)
                .fromAirport(saved.getFromAirport())
                .toAirport(saved.getToAirport())
                .departureDate(saved.getDepartureDate())
                .returnDate(saved.getReturnDate())
                .passengers(saved.getPassengers())
                .cabinClass(saved.getCabinClass())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightSearchResponseDto> getUserFlightSearches(Long userId) {
        return flightSearchRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> FlightSearchResponseDto.builder()
                        .id(s.getId())
                        .userId(s.getUser() != null ? s.getUser().getId() : null)
                        .fromAirport(s.getFromAirport())
                        .toAirport(s.getToAirport())
                        .departureDate(s.getDepartureDate())
                        .returnDate(s.getReturnDate())
                        .passengers(s.getPassengers())
                        .cabinClass(s.getCabinClass())
                        .createdAt(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RideSearchResponseDto recordRideSearch(RideSearchRequestDto request) {
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        RideSearch search = RideSearch.builder()
                .user(user)
                .pickupLocation(request.getPickupLocation().trim())
                .dropLocation(request.getDropLocation().trim())
                .rideType(request.getRideType())
                .build();

        RideSearch saved = rideSearchRepository.save(search);
        return RideSearchResponseDto.builder()
                .id(saved.getId())
                .userId(saved.getUser() != null ? saved.getUser().getId() : null)
                .pickupLocation(saved.getPickupLocation())
                .dropLocation(saved.getDropLocation())
                .rideType(saved.getRideType())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideSearchResponseDto> getUserRideSearches(Long userId) {
        return rideSearchRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> RideSearchResponseDto.builder()
                        .id(r.getId())
                        .userId(r.getUser() != null ? r.getUser().getId() : null)
                        .pickupLocation(r.getPickupLocation())
                        .dropLocation(r.getDropLocation())
                        .rideType(r.getRideType())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDashboardResponseDto.RecentActivityDto> getUserRecentActivity(Long userId, int limit) {
        List<UserDashboardResponseDto.RecentActivityDto> activity = new ArrayList<>();

        // 1. Search histories (shopping & comparison)
        List<SearchHistory> searches = searchHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        for (SearchHistory s : searches) {
            boolean isCompare = s.getSearchType() == SearchType.PRODUCT_COMPARE;
            String title = isCompare ? "Compared " + s.getQuery() : "Searched " + s.getQuery();
            String desc = s.getDetails() != null ? s.getDetails() : (isCompare ? "Amazon vs Flipkart vs Croma" : "Product catalog search");
            String actionUrl = s.getTargetUrl() != null ? s.getTargetUrl() : "/shopping?q=" + s.getQuery();
            String actionLabel = isCompare ? "Compare Again" : "Search Again";

            activity.add(UserDashboardResponseDto.RecentActivityDto.builder()
                    .id(s.getId())
                    .title(title)
                    .description(desc)
                    .activityType(s.getSearchType().name())
                    .actionUrl(actionUrl)
                    .actionLabel(actionLabel)
                    .createdAt(s.getCreatedAt())
                    .build());
        }

        // 2. Flight searches
        List<FlightSearch> flights = flightSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (FlightSearch f : flights) {
            activity.add(UserDashboardResponseDto.RecentActivityDto.builder()
                    .id(f.getId())
                    .title("Searched " + f.getFromAirport() + " → " + f.getToAirport() + " flights")
                    .description(f.getCabinClass() + " • " + (f.getDepartureDate() != null ? f.getDepartureDate().toString() : "Flexible"))
                    .activityType("FLIGHT_SEARCH")
                    .actionUrl("/flights?from=" + f.getFromAirport() + "&to=" + f.getToAirport())
                    .actionLabel("Search Again")
                    .createdAt(f.getCreatedAt())
                    .build());
        }

        // 3. Ride searches
        List<RideSearch> rides = rideSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (RideSearch r : rides) {
            activity.add(UserDashboardResponseDto.RecentActivityDto.builder()
                    .id(r.getId())
                    .title("Compared ride options")
                    .description(r.getPickupLocation() + " → " + r.getDropLocation())
                    .activityType("RIDE_SEARCH")
                    .actionUrl("/rides?pickup=" + r.getPickupLocation() + "&dest=" + r.getDropLocation())
                    .actionLabel("Compare Again")
                    .createdAt(r.getCreatedAt())
                    .build());
        }

        // Sort all by timestamp descending and cap to limit
        return activity.stream()
                .sorted(Comparator.comparing(UserDashboardResponseDto.RecentActivityDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : 10)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDashboardResponseDto.RecentComparisonDto> getUserRecentComparisons(Long userId, int limit) {
        List<SearchHistory> compares = searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return compares.stream()
                .filter(s -> s.getSearchType() == SearchType.PRODUCT_COMPARE || s.getSearchType() == SearchType.SHOPPING)
                .limit(limit > 0 ? limit : 5)
                .map(s -> UserDashboardResponseDto.RecentComparisonDto.builder()
                        .id(s.getId())
                        .title(s.getQuery())
                        .merchants(s.getDetails() != null ? s.getDetails() : "Amazon vs Flipkart vs Croma")
                        .compareUrl(s.getTargetUrl() != null ? s.getTargetUrl() : "/shopping?q=" + s.getQuery())
                        .lastCompared(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
