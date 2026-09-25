package com.comparehub.service;

import com.comparehub.dto.SearchHistoryRequestDto;
import com.comparehub.dto.SearchHistoryResponseDto;
import com.comparehub.dto.UserDashboardResponseDto;
import com.comparehub.model.*;
import com.comparehub.repository.FlightSearchRepository;
import com.comparehub.repository.RideSearchRepository;
import com.comparehub.repository.SearchHistoryRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.SearchHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private FlightSearchRepository flightSearchRepository;

    @Mock
    private RideSearchRepository rideSearchRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchHistoryServiceImpl searchHistoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@example.com").name("User One").build();
    }

    @Test
    @DisplayName("Should record search with comparison details and target URL")
    void testRecordSearch_withDetails() {
        SearchHistoryRequestDto request = SearchHistoryRequestDto.builder()
                .userId(1L)
                .query("Samsung Galaxy S24")
                .searchType(SearchType.PRODUCT_COMPARE)
                .details("Amazon vs Flipkart vs Croma")
                .targetUrl("/shopping?q=Samsung+Galaxy+S24")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(searchHistoryRepository.countByUserId(1L)).thenReturn(5L);

        SearchHistory saved = SearchHistory.builder()
                .id(101L)
                .user(testUser)
                .query("Samsung Galaxy S24")
                .searchType(SearchType.PRODUCT_COMPARE)
                .details("Amazon vs Flipkart vs Croma")
                .targetUrl("/shopping?q=Samsung+Galaxy+S24")
                .createdAt(Instant.now())
                .build();

        when(searchHistoryRepository.save(any(SearchHistory.class))).thenReturn(saved);

        SearchHistoryResponseDto response = searchHistoryService.recordSearch(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getSearchType()).isEqualTo(SearchType.PRODUCT_COMPARE);
        assertThat(response.getDetails()).isEqualTo("Amazon vs Flipkart vs Croma");
        assertThat(response.getTargetUrl()).isEqualTo("/shopping?q=Samsung+Galaxy+S24");
    }

    @Test
    @DisplayName("Should prune oldest history entries when count reaches maximum (Bounded Table Growth)")
    void testRecordSearch_prunesOldestWhenMaxReached() {
        SearchHistoryRequestDto request = SearchHistoryRequestDto.builder()
                .userId(1L)
                .query("New Search Query")
                .searchType(SearchType.SHOPPING)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Current count is 100 (at limit)
        when(searchHistoryRepository.countByUserId(1L)).thenReturn(100L);

        SearchHistory oldest = SearchHistory.builder().id(1L).build();
        when(searchHistoryRepository.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(oldest));

        SearchHistory saved = SearchHistory.builder()
                .id(101L)
                .user(testUser)
                .query("New Search Query")
                .searchType(SearchType.SHOPPING)
                .build();
        when(searchHistoryRepository.save(any(SearchHistory.class))).thenReturn(saved);

        searchHistoryService.recordSearch(request);

        // Verify oldest was pruned to keep table bounded
        verify(searchHistoryRepository, times(1)).delete(oldest);
        verify(searchHistoryRepository, times(1)).save(any(SearchHistory.class));
    }

    @Test
    @DisplayName("Should aggregate recent activity across products, flights, and rides")
    void testGetUserRecentActivity() {
        SearchHistory s = SearchHistory.builder()
                .id(1L)
                .query("iPhone 15")
                .searchType(SearchType.PRODUCT_COMPARE)
                .details("Amazon vs Flipkart")
                .createdAt(Instant.now())
                .build();

        when(searchHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(s));
        when(flightSearchRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(rideSearchRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<UserDashboardResponseDto.RecentActivityDto> activity = searchHistoryService.getUserRecentActivity(1L, 5);

        assertThat(activity).hasSize(1);
        assertThat(activity.get(0).getTitle()).isEqualTo("Compared iPhone 15");
        assertThat(activity.get(0).getActionLabel()).isEqualTo("Compare Again");
    }
}
