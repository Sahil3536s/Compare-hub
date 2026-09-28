package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.model.User;
import com.comparehub.model.UserPaymentPreference;
import com.comparehub.repository.UserPaymentPreferenceRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.PaymentOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOfferServiceImpl implements PaymentOfferService {

    private final UserPaymentPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    public PaymentOfferSummaryDto evaluatePaymentOffers(
            BigDecimal basePrice,
            BigDecimal additionalFees,
            String merchant,
            String category,
            PaymentPreferenceDto userPreference) {

        BigDecimal safeBasePrice = basePrice != null ? basePrice : BigDecimal.ZERO;
        BigDecimal safeAdditionalFees = additionalFees != null ? additionalFees : BigDecimal.ZERO;
        BigDecimal standardPrice = safeBasePrice.add(safeAdditionalFees);

        PaymentPreferenceDto safePref = userPreference != null ? userPreference : getDefaultPreferences(null);

        List<PaymentOfferDto> rawOffers = getAvailableOffers(merchant, category, safeBasePrice);
        List<PaymentOfferDto> evaluatedOffers = new ArrayList<>();

        for (PaymentOfferDto offer : rawOffers) {
            BigDecimal discount = offer.getDiscountAmount() != null ? offer.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal effective = standardPrice.subtract(discount);
            if (effective.compareTo(BigDecimal.ZERO) < 0) {
                effective = BigDecimal.ZERO;
            }
            offer.setEffectivePrice(effective);

            // Determine eligibility
            evaluateEligibility(offer, safePref, safeBasePrice);
            evaluatedOffers.add(offer);
        }

        // Sort offers by effective price ascending
        evaluatedOffers.sort(Comparator.comparing(PaymentOfferDto::getEffectivePrice));

        // Find best eligible offer (lowest effective price among ELIGIBLE / POSSIBLY_ELIGIBLE)
        PaymentOfferDto bestOffer = evaluatedOffers.stream()
                .filter(o -> o.getEligibilityStatus() == PaymentEligibilityStatus.ELIGIBLE
                        || o.getEligibilityStatus() == PaymentEligibilityStatus.POSSIBLY_ELIGIBLE)
                .min(Comparator.comparing(PaymentOfferDto::getEffectivePrice))
                .orElse(null);

        BigDecimal bestEligiblePrice = bestOffer != null && bestOffer.getEffectivePrice() != null
                ? bestOffer.getEffectivePrice()
                : standardPrice;

        BigDecimal maxSavings = standardPrice.subtract(bestEligiblePrice);
        if (maxSavings.compareTo(BigDecimal.ZERO) < 0) {
            maxSavings = BigDecimal.ZERO;
        }

        return PaymentOfferSummaryDto.builder()
                .standardPrice(standardPrice)
                .bestEligiblePrice(bestEligiblePrice)
                .maxSavings(maxSavings)
                .bestOffer(bestOffer)
                .offers(evaluatedOffers)
                .userPreference(safePref)
                .securityNote("??? CompareHub never collects or stores card numbers, CVVs, PINs, or OTPs. Only bank preferences are used to find matching discounts.")
                .build();
    }

    private void evaluateEligibility(PaymentOfferDto offer, PaymentPreferenceDto pref, BigDecimal basePrice) {
        // 1. Check minimum purchase condition
        if (offer.getMinPurchaseAmount() != null && basePrice.compareTo(offer.getMinPurchaseAmount()) < 0) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
            offer.setEligibilityReason("Minimum purchase of ?" + offer.getMinPurchaseAmount() + " required");
            return;
        }

        String prefBank = pref != null && pref.getPreferredBank() != null ? pref.getPreferredBank().trim().toUpperCase() : "ALL";
        String prefCardType = pref != null && pref.getPreferredCardType() != null ? pref.getPreferredCardType().trim().toUpperCase() : "ALL";
        boolean prefUpi = pref == null || Boolean.TRUE.equals(pref.getHasUpi());
        boolean prefWallet = pref != null && Boolean.TRUE.equals(pref.getHasWallet());
        String prefWalletType = pref != null && pref.getPreferredWallet() != null ? pref.getPreferredWallet().trim().toUpperCase() : "NONE";

        // If universal coupon or store discount
        if (offer.getPaymentType() == PaymentOfferType.COUPON || offer.getPaymentType() == PaymentOfferType.INSTANT_DISCOUNT) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.ELIGIBLE);
            offer.setEligibilityReason("Universal store offer applicable for all customers");
            return;
        }

        // UPI offer
        if (offer.getPaymentType() == PaymentOfferType.UPI) {
            if (prefUpi) {
                offer.setEligibilityStatus(PaymentEligibilityStatus.ELIGIBLE);
                offer.setEligibilityReason("Matches your UPI payment preference");
            } else {
                offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
                offer.setEligibilityReason("UPI not enabled in your payment preferences");
            }
            return;
        }

        // Wallet offer
        if (offer.getPaymentType() == PaymentOfferType.WALLET) {
            if (prefWallet && ("ALL".equals(prefWalletType) || offer.getBank().equalsIgnoreCase(prefWalletType))) {
                offer.setEligibilityStatus(PaymentEligibilityStatus.ELIGIBLE);
                offer.setEligibilityReason("Matches your " + offer.getBank() + " Wallet preference");
            } else if (!prefWallet) {
                offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
                offer.setEligibilityReason("Wallet payment not selected in preferences");
            } else {
                offer.setEligibilityStatus(PaymentEligibilityStatus.POSSIBLY_ELIGIBLE);
                offer.setEligibilityReason("Requires " + offer.getBank() + " Wallet");
            }
            return;
        }

        // Card Offers (CREDIT_CARD / DEBIT_CARD)
        boolean isCredit = offer.getPaymentType() == PaymentOfferType.CREDIT_CARD;
        boolean isDebit = offer.getPaymentType() == PaymentOfferType.DEBIT_CARD;

        // If user has not specified bank preference ("ALL")
        if ("ALL".equals(prefBank) && "ALL".equals(prefCardType)) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.POSSIBLY_ELIGIBLE);
            offer.setEligibilityReason("Requires " + offer.getBank() + " " + (isCredit ? "Credit Card" : "Debit Card") + " - verify your card");
            return;
        }

        // Check card type mismatch
        if (isCredit && "DEBIT_CARD".equals(prefCardType)) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
            offer.setEligibilityReason("Requires a Credit Card (your preference is Debit Card)");
            return;
        }
        if (isDebit && "CREDIT_CARD".equals(prefCardType)) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
            offer.setEligibilityReason("Requires a Debit Card (your preference is Credit Card)");
            return;
        }

        // Check bank match
        if (!"ALL".equals(prefBank) && !offer.getBank().equalsIgnoreCase(prefBank) && !offer.getBank().equalsIgnoreCase("ALL")) {
            offer.setEligibilityStatus(PaymentEligibilityStatus.NOT_ELIGIBLE);
            offer.setEligibilityReason("Requires " + offer.getBank() + " card (your preferred bank is " + prefBank + ")");
            return;
        }

        offer.setEligibilityStatus(PaymentEligibilityStatus.ELIGIBLE);
        offer.setEligibilityReason("Matches your " + offer.getBank() + " " + (isCredit ? "Credit Card" : "Debit Card") + " preference");
    }

    @Override
    public List<PaymentOfferDto> getAvailableOffers(String merchant, String category, BigDecimal basePrice) {
        List<PaymentOfferDto> list = new ArrayList<>();
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return list;
        }

        String m = merchant != null ? merchant.toLowerCase().trim() : "";

        if (m.contains("amazon")) {
            if (basePrice.compareTo(BigDecimal.valueOf(50000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("AMZ_HDFC_CC_50K")
                        .bank("HDFC")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("HDFC Credit Card Instant Discount")
                        .description("Flat ?3,000 instant discount on orders above ?50,000")
                        .discountAmount(BigDecimal.valueOf(3000))
                        .minPurchaseAmount(BigDecimal.valueOf(50000))
                        .isConditional(true)
                        .terms("Valid on HDFC Bank Credit Card non-EMI and EMI transactions.")
                        .build());
                list.add(PaymentOfferDto.builder()
                        .id("AMZ_ICICI_CC_40K")
                        .bank("ICICI")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("ICICI Credit Card Instant Discount")
                        .description("Flat ?2,500 instant discount on ICICI Credit Cards")
                        .discountAmount(BigDecimal.valueOf(2500))
                        .minPurchaseAmount(BigDecimal.valueOf(40000))
                        .isConditional(true)
                        .terms("Valid on ICICI Bank Credit Cards.")
                        .build());
            } else if (basePrice.compareTo(BigDecimal.valueOf(20000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("AMZ_SBI_CC_20K")
                        .bank("SBI")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("SBI Credit Card Offer")
                        .description("Flat ?1,500 off on SBI Credit Card transactions")
                        .discountAmount(BigDecimal.valueOf(1500))
                        .minPurchaseAmount(BigDecimal.valueOf(20000))
                        .isConditional(true)
                        .terms("Applicable on SBI Card transactions over ?20,000.")
                        .build());
            }

            if (basePrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("AMZ_UPI_GEN")
                        .bank("ALL")
                        .paymentType(PaymentOfferType.UPI)
                        .title("Amazon Pay UPI / Any UPI Discount")
                        .description("Instant ?250 discount using UPI payment")
                        .discountAmount(BigDecimal.valueOf(250))
                        .minPurchaseAmount(BigDecimal.valueOf(1000))
                        .isConditional(false)
                        .terms("Valid once per user using any verified UPI ID.")
                        .build());
            }
        } else if (m.contains("flipkart")) {
            if (basePrice.compareTo(BigDecimal.valueOf(30000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("FK_AXIS_CC_30K")
                        .bank("AXIS")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("Flipkart Axis Bank Credit Card")
                        .description("Flat ?2,500 instant discount on Axis Bank Credit Cards")
                        .discountAmount(BigDecimal.valueOf(2500))
                        .minPurchaseAmount(BigDecimal.valueOf(30000))
                        .isConditional(true)
                        .terms("Valid on Axis Bank Credit Cards.")
                        .build());
                list.add(PaymentOfferDto.builder()
                        .id("FK_HDFC_CC_30K")
                        .bank("HDFC")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("HDFC Bank Credit Card Offer")
                        .description("Flat ?2,000 instant discount on HDFC Credit Cards")
                        .discountAmount(BigDecimal.valueOf(2000))
                        .minPurchaseAmount(BigDecimal.valueOf(30000))
                        .isConditional(true)
                        .terms("Applicable on HDFC Credit Cards.")
                        .build());
            } else if (basePrice.compareTo(BigDecimal.valueOf(10000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("FK_ICICI_CARD_10K")
                        .bank("ICICI")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("ICICI Bank Cards Discount")
                        .description("Flat ?1,000 instant discount on ICICI Cards")
                        .discountAmount(BigDecimal.valueOf(1000))
                        .minPurchaseAmount(BigDecimal.valueOf(10000))
                        .isConditional(true)
                        .terms("Valid on ICICI Credit & Debit Cards.")
                        .build());
            }

            if (basePrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("FK_UPI_GEN")
                        .bank("ALL")
                        .paymentType(PaymentOfferType.UPI)
                        .title("Flipkart UPI Instant Savings")
                        .description("Instant ?200 off on UPI transactions")
                        .discountAmount(BigDecimal.valueOf(200))
                        .minPurchaseAmount(BigDecimal.valueOf(1000))
                        .isConditional(false)
                        .terms("Valid on any verified UPI payment.")
                        .build());
            }
        } else if (m.contains("croma")) {
            if (basePrice.compareTo(BigDecimal.valueOf(25000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("CR_TATA_HDFC_25K")
                        .bank("HDFC")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("Tata Neu HDFC Credit Card")
                        .description("Flat ?2,000 instant discount on Tata Neu HDFC Cards")
                        .discountAmount(BigDecimal.valueOf(2000))
                        .minPurchaseAmount(BigDecimal.valueOf(25000))
                        .isConditional(true)
                        .terms("Valid on Croma online checkout.")
                        .build());
                list.add(PaymentOfferDto.builder()
                        .id("CR_ICICI_CC_25K")
                        .bank("ICICI")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("ICICI Credit Card Instant Off")
                        .description("Flat ?1,500 off on ICICI Credit Cards")
                        .discountAmount(BigDecimal.valueOf(1500))
                        .minPurchaseAmount(BigDecimal.valueOf(25000))
                        .isConditional(true)
                        .terms("Valid on minimum purchase of ?25,000.")
                        .build());
            }
            if (basePrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("CR_UPI_GEN")
                        .bank("ALL")
                        .paymentType(PaymentOfferType.UPI)
                        .title("UPI Instant Payment Offer")
                        .description("Instant ?250 discount with UPI")
                        .discountAmount(BigDecimal.valueOf(250))
                        .minPurchaseAmount(BigDecimal.valueOf(1000))
                        .isConditional(false)
                        .terms("Applicable across all UPI apps.")
                        .build());
            }
        } else {
            // Generic Merchant
            if (basePrice.compareTo(BigDecimal.valueOf(10000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("GEN_HDFC_10K")
                        .bank("HDFC")
                        .paymentType(PaymentOfferType.CREDIT_CARD)
                        .title("HDFC Credit Card Offer")
                        .description("Flat ?1,000 off on HDFC Credit Cards")
                        .discountAmount(BigDecimal.valueOf(1000))
                        .minPurchaseAmount(BigDecimal.valueOf(10000))
                        .isConditional(true)
                        .terms("Standard merchant bank offer.")
                        .build());
            }
            if (basePrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                list.add(PaymentOfferDto.builder()
                        .id("GEN_UPI")
                        .bank("ALL")
                        .paymentType(PaymentOfferType.UPI)
                        .title("Instant UPI Discount")
                        .description("Flat ?150 off with UPI")
                        .discountAmount(BigDecimal.valueOf(150))
                        .minPurchaseAmount(BigDecimal.valueOf(1000))
                        .isConditional(false)
                        .terms("Instant discount on checkout.")
                        .build());
            }
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentPreferenceDto getUserPreferences(Long userId) {
        if (userId == null) {
            return getDefaultPreferences(null);
        }

        return preferenceRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseGet(() -> getDefaultPreferences(userId));
    }

    @Override
    @Transactional
    public PaymentPreferenceDto saveUserPreferences(Long userId, PaymentPreferenceDto dto) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to save payment preferences");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        UserPaymentPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(UserPaymentPreference.builder().user(user).build());

        pref.setPreferredBank(dto.getPreferredBank() != null ? dto.getPreferredBank().trim().toUpperCase() : "ALL");
        pref.setPreferredCardType(dto.getPreferredCardType() != null ? dto.getPreferredCardType().trim().toUpperCase() : "ALL");
        pref.setHasUpi(dto.getHasUpi() != null ? dto.getHasUpi() : true);
        pref.setHasWallet(dto.getHasWallet() != null ? dto.getHasWallet() : false);
        pref.setPreferredWallet(dto.getPreferredWallet() != null ? dto.getPreferredWallet().trim().toUpperCase() : "NONE");

        UserPaymentPreference saved = preferenceRepository.save(pref);
        return mapToDto(saved);
    }

    private PaymentPreferenceDto mapToDto(UserPaymentPreference pref) {
        return PaymentPreferenceDto.builder()
                .userId(pref.getUser() != null ? pref.getUser().getId() : null)
                .preferredBank(pref.getPreferredBank())
                .preferredCardType(pref.getPreferredCardType())
                .hasUpi(pref.getHasUpi())
                .hasWallet(pref.getHasWallet())
                .preferredWallet(pref.getPreferredWallet())
                .build();
    }

    private PaymentPreferenceDto getDefaultPreferences(Long userId) {
        return PaymentPreferenceDto.builder()
                .userId(userId)
                .preferredBank("ALL")
                .preferredCardType("ALL")
                .hasUpi(true)
                .hasWallet(false)
                .preferredWallet("NONE")
                .build();
    }
}
