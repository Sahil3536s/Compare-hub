package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductRankingWeightsDto;
import com.comparehub.dto.RankingSummaryDto;
import com.comparehub.dto.UserRankingPreferenceDto;
import com.comparehub.model.User;
import com.comparehub.model.UserRankingPreference;
import com.comparehub.repository.UserRankingPreferenceRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.PersonalizedRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalizedRankingServiceTest {

    @Mock
    private UserRankingPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    private PersonalizedRankingService personalizedRankingService;

    @BeforeEach
    void setUp() {
        personalizedRankingService = new PersonalizedRankingServiceImpl(preferenceRepository, userRepository);
    }

    @Test
    @DisplayName("Should rank cheaper offer higher when SAVE_MONEY preset (Price 70%) is applied")
    void testRankWithSaveMoneyPreset() {
        // Offer A: ₹50,000, 4.2★, Standard Delivery
        NormalizedProductOfferDto offerA = NormalizedProductOfferDto.builder()
                .productName("Laptop Model X")
                .merchant("Amazon")
                .price(new BigDecimal("50000"))
                .rating(4.2)
                .delivery("Standard Delivery")
                .discountPercent(5)
                .inStock(true)
                .build();

        // Offer B: ₹58,000, 4.9★, Express Delivery
        NormalizedProductOfferDto offerB = NormalizedProductOfferDto.builder()
                .productName("Laptop Model X")
                .merchant("Flipkart")
                .price(new BigDecimal("58000"))
                .rating(4.9)
                .delivery("Same Day Delivery")
                .discountPercent(15)
                .inStock(true)
                .build();

        // Save Money weights: Price 70%, Delivery 10%, Rating 10%, Reliability 10%
        ProductRankingWeightsDto saveMoneyWeights = ProductRankingWeightsDto.builder()
                .price(70)
                .rating(10)
                .delivery(10)
                .discount(0)
                .reliability(10)
                .build();

        List<NormalizedProductOfferDto> ranked = personalizedRankingService.rankProductsWithCustomWeights(
                List.of(offerA, offerB), saveMoneyWeights, "best");

        assertThat(ranked.get(0).getMerchant()).isEqualTo("Amazon");
        assertThat(ranked.get(0).getIsBestValue()).isTrue();

        RankingSummaryDto summary = personalizedRankingService.getPersonalizedRankingSummary(ranked, saveMoneyWeights);
        assertThat(summary.getExplanation()).contains("Price (70%)");
    }

    @Test
    @DisplayName("Should rank higher-rated offer higher when QUALITY_FIRST preset (Rating 50%) is applied")
    void testRankWithQualityPreset() {
        NormalizedProductOfferDto offerA = NormalizedProductOfferDto.builder()
                .productName("Headphones")
                .merchant("Amazon")
                .price(new BigDecimal("10000"))
                .rating(3.8)
                .delivery("Tomorrow")
                .inStock(true)
                .build();

        NormalizedProductOfferDto offerB = NormalizedProductOfferDto.builder()
                .productName("Headphones")
                .merchant("Flipkart")
                .price(new BigDecimal("11500"))
                .rating(4.9)
                .delivery("Tomorrow")
                .inStock(true)
                .build();

        // Quality weights: Rating 50%, Reliability 20%, Price 20%, Delivery 10%
        ProductRankingWeightsDto qualityWeights = ProductRankingWeightsDto.builder()
                .price(20)
                .rating(50)
                .delivery(10)
                .discount(0)
                .reliability(20)
                .build();

        List<NormalizedProductOfferDto> ranked = personalizedRankingService.rankProductsWithCustomWeights(
                List.of(offerA, offerB), qualityWeights, "best");

        assertThat(ranked.get(0).getMerchant()).isEqualTo("Flipkart");
        assertThat(ranked.get(0).getIsBestValue()).isTrue();
    }

    @Test
    @DisplayName("Should save user ranking preference to repository")
    void testSaveUserPreferences() {
        User user = User.builder().id(1L).email("user@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserRankingPreference.class))).thenAnswer(i -> {
            UserRankingPreference p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        UserRankingPreferenceDto inputDto = UserRankingPreferenceDto.builder()
                .preset("CHEAPEST")
                .product(ProductRankingWeightsDto.builder().price(70).rating(10).delivery(10).discount(5).reliability(5).build())
                .build();

        UserRankingPreferenceDto saved = personalizedRankingService.saveUserPreferences(1L, inputDto);

        assertThat(saved).isNotNull();
        assertThat(saved.getPreset()).isEqualTo("CHEAPEST");
        verify(preferenceRepository).save(any(UserRankingPreference.class));
    }
}
