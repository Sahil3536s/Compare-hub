package com.comparehub.service;

import com.comparehub.dto.*;
import com.comparehub.model.User;
import com.comparehub.model.UserPaymentPreference;
import com.comparehub.repository.UserPaymentPreferenceRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.PaymentOfferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOfferServiceTest {

    @Mock
    private UserPaymentPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentOfferService paymentOfferService;

    @BeforeEach
    void setUp() {
        paymentOfferService = new PaymentOfferServiceImpl(preferenceRepository, userRepository);
    }

    @Test
    @DisplayName("Should evaluate HDFC Credit Card offer as ELIGIBLE when user has HDFC Credit preference")
    void testHdfcCreditOfferEligible() {
        PaymentPreferenceDto userPref = PaymentPreferenceDto.builder()
                .preferredBank("HDFC")
                .preferredCardType("CREDIT_CARD")
                .hasUpi(true)
                .build();

        // High-value Amazon product (?62,999)
        BigDecimal basePrice = new BigDecimal("62999");
        BigDecimal fees = BigDecimal.ZERO;

        PaymentOfferSummaryDto summary = paymentOfferService.evaluatePaymentOffers(
                basePrice, fees, "Amazon", "Electronics", userPref);

        assertThat(summary).isNotNull();
        assertThat(summary.getStandardPrice()).isEqualByComparingTo(new BigDecimal("62999"));
        // HDFC offer gives ?3,000 off -> 62999 - 3000 = 59999
        assertThat(summary.getBestEligiblePrice()).isEqualByComparingTo(new BigDecimal("59999"));
        assertThat(summary.getMaxSavings()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(summary.getBestOffer()).isNotNull();
        assertThat(summary.getBestOffer().getBank()).isEqualTo("HDFC");
        assertThat(summary.getBestOffer().getEligibilityStatus()).isEqualTo(PaymentEligibilityStatus.ELIGIBLE);
        assertThat(summary.getBestOffer().getEligibilityReason()).contains("Matches your HDFC Credit Card preference");
    }

    @Test
    @DisplayName("Should mark HDFC Credit offer as NOT_ELIGIBLE if user preferences specify ICICI only")
    void testCardMismatchNotEligible() {
        PaymentPreferenceDto userPref = PaymentPreferenceDto.builder()
                .preferredBank("ICICI")
                .preferredCardType("CREDIT_CARD")
                .hasUpi(false)
                .build();

        BigDecimal basePrice = new BigDecimal("62999");
        BigDecimal fees = BigDecimal.ZERO;

        PaymentOfferSummaryDto summary = paymentOfferService.evaluatePaymentOffers(
                basePrice, fees, "Amazon", "Electronics", userPref);

        assertThat(summary).isNotNull();
        // HDFC should be NOT_ELIGIBLE, ICICI offer (?2,500 off) should be ELIGIBLE -> 62999 - 2500 = 60499
        assertThat(summary.getBestEligiblePrice()).isEqualByComparingTo(new BigDecimal("60499"));
        assertThat(summary.getMaxSavings()).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(summary.getBestOffer().getBank()).isEqualTo("ICICI");
        assertThat(summary.getBestOffer().getEligibilityStatus()).isEqualTo(PaymentEligibilityStatus.ELIGIBLE);

        PaymentOfferDto hdfcOffer = summary.getOffers().stream()
                .filter(o -> "HDFC".equals(o.getBank()))
                .findFirst()
                .orElse(null);
        assertThat(hdfcOffer).isNotNull();
        assertThat(hdfcOffer.getEligibilityStatus()).isEqualTo(PaymentEligibilityStatus.NOT_ELIGIBLE);
        assertThat(hdfcOffer.getEligibilityReason()).contains("Requires HDFC card (your preferred bank is ICICI)");
    }

    @Test
    @DisplayName("Should mark bank offers as POSSIBLY_ELIGIBLE when no specific bank preference is configured")
    void testDefaultPreferencesPossiblyEligible() {
        PaymentPreferenceDto userPref = PaymentPreferenceDto.builder()
                .preferredBank("ALL")
                .preferredCardType("ALL")
                .hasUpi(true)
                .build();

        BigDecimal basePrice = new BigDecimal("62999");

        PaymentOfferSummaryDto summary = paymentOfferService.evaluatePaymentOffers(
                basePrice, BigDecimal.ZERO, "Amazon", "Electronics", userPref);

        assertThat(summary).isNotNull();
        // Top bank offer is POSSIBLY_ELIGIBLE
        PaymentOfferDto bestOffer = summary.getBestOffer();
        assertThat(bestOffer).isNotNull();
        assertThat(bestOffer.getEligibilityStatus()).isEqualTo(PaymentEligibilityStatus.POSSIBLY_ELIGIBLE);
        assertThat(bestOffer.getEligibilityReason()).contains("Requires HDFC Credit Card - verify your card");

        // UPI offer is universally ELIGIBLE
        PaymentOfferDto upiOffer = summary.getOffers().stream()
                .filter(o -> o.getPaymentType() == PaymentOfferType.UPI)
                .findFirst()
                .orElse(null);
        assertThat(upiOffer).isNotNull();
        assertThat(upiOffer.getEligibilityStatus()).isEqualTo(PaymentEligibilityStatus.ELIGIBLE);
    }

    @Test
    @DisplayName("Should mark offer as NOT_ELIGIBLE if product price is below minimum purchase amount")
    void testBelowMinimumPurchaseThreshold() {
        PaymentPreferenceDto userPref = PaymentPreferenceDto.builder()
                .preferredBank("HDFC")
                .preferredCardType("CREDIT_CARD")
                .hasUpi(true)
                .build();

        // Low price product (?500), where HDFC min spend is ?50,000 and UPI min spend is ?1,000
        BigDecimal basePrice = new BigDecimal("500");

        PaymentOfferSummaryDto summary = paymentOfferService.evaluatePaymentOffers(
                basePrice, BigDecimal.ZERO, "Amazon", "Electronics", userPref);

        assertThat(summary).isNotNull();
        assertThat(summary.getBestEligiblePrice()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(summary.getMaxSavings()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should successfully persist and retrieve user payment preferences without sensitive card data")
    void testSaveAndGetUserPreferences() {
        Long userId = 42L;
        User mockUser = User.builder().id(userId).email("user@test.com").build();
        UserPaymentPreference mockEntity = UserPaymentPreference.builder()
                .id(1L)
                .user(mockUser)
                .preferredBank("SBI")
                .preferredCardType("DEBIT_CARD")
                .hasUpi(true)
                .hasWallet(true)
                .preferredWallet("PAYTM")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(preferenceRepository.save(any(UserPaymentPreference.class))).thenReturn(mockEntity);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(mockEntity));

        PaymentPreferenceDto input = PaymentPreferenceDto.builder()
                .preferredBank("SBI")
                .preferredCardType("DEBIT_CARD")
                .hasUpi(true)
                .hasWallet(true)
                .preferredWallet("PAYTM")
                .build();

        PaymentPreferenceDto saved = paymentOfferService.saveUserPreferences(userId, input);
        assertThat(saved.getPreferredBank()).isEqualTo("SBI");
        assertThat(saved.getPreferredCardType()).isEqualTo("DEBIT_CARD");

        PaymentPreferenceDto retrieved = paymentOfferService.getUserPreferences(userId);
        assertThat(retrieved.getPreferredBank()).isEqualTo("SBI");
        assertThat(retrieved.getPreferredWallet()).isEqualTo("PAYTM");
    }
}
