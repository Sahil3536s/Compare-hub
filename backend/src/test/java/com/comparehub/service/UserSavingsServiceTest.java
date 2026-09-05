package com.comparehub.service;

import com.comparehub.dto.SavingsEventDto;
import com.comparehub.dto.SavingsSummaryDto;
import com.comparehub.model.SavingsEvent;
import com.comparehub.model.SavingsEventType;
import com.comparehub.repository.*;
import com.comparehub.service.impl.UserSavingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSavingsServiceTest {

    @Mock
    private SavingsEventRepository savingsEventRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private SavedProductRepository savedProductRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private UserRepository userRepository;

    private UserSavingsService userSavingsService;

    @BeforeEach
    void setUp() {
        userSavingsService = new UserSavingsServiceImpl(
                savingsEventRepository,
                searchHistoryRepository,
                savedProductRepository,
                priceAlertRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("Should aggregate potential and confirmed savings correctly")
    void testGetSavingsSummary() {
        SavingsEvent event1 = SavingsEvent.builder()
                .id(1L)
                .title("Laptop")
                .category("Laptops")
                .eventType(SavingsEventType.CONFIRMED)
                .selectedPrice(BigDecimal.valueOf(60000))
                .baselinePrice(BigDecimal.valueOf(67200))
                .savingAmount(BigDecimal.valueOf(7200))
                .createdAt(Instant.now())
                .build();

        SavingsEvent event2 = SavingsEvent.builder()
                .id(2L)
                .title("Smartphone")
                .category("Smartphones")
                .eventType(SavingsEventType.POTENTIAL)
                .selectedPrice(BigDecimal.valueOf(54999))
                .baselinePrice(BigDecimal.valueOf(59279))
                .savingAmount(BigDecimal.valueOf(4280))
                .createdAt(Instant.now())
                .build();

        when(savingsEventRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(event1, event2));
        when(searchHistoryRepository.countByUserId(1L)).thenReturn(37L);
        when(savedProductRepository.countByUserId(1L)).thenReturn(5L);
        when(priceAlertRepository.countByUserId(1L)).thenReturn(4L);

        SavingsSummaryDto summary = userSavingsService.getSavingsSummary(1L);

        assertNotNull(summary);
        assertEquals(BigDecimal.valueOf(7200), summary.getTotalConfirmedSavings());
        assertEquals(BigDecimal.valueOf(4280), summary.getTotalPotentialSavings());
        assertEquals(37, summary.getTotalComparisons());
        assertNotNull(summary.getLargestSaving());
        assertEquals("Laptop", summary.getLargestSaving().getTitle());
        assertEquals(BigDecimal.valueOf(7200), summary.getLargestSaving().getAmount());
    }

    @Test
    @DisplayName("Should confirm potential savings event")
    void testConfirmSavings() {
        SavingsEvent event = SavingsEvent.builder()
                .id(1L)
                .title("Smartphone")
                .eventType(SavingsEventType.POTENTIAL)
                .savingAmount(BigDecimal.valueOf(4280))
                .build();

        when(savingsEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(savingsEventRepository.save(any(SavingsEvent.class))).thenAnswer(i -> i.getArgument(0));

        SavingsEventDto confirmed = userSavingsService.confirmSavings(1L);

        assertNotNull(confirmed);
        assertEquals("CONFIRMED", confirmed.getEventType());
    }
}
