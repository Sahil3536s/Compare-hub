package com.comparehub.service.impl;

import com.comparehub.dto.AppliedOfferDto;
import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.PaymentOfferSummaryDto;
import com.comparehub.service.OfferEligibilityService;
import com.comparehub.service.PaymentOfferService;
import com.comparehub.service.TrueCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrueCostServiceImpl implements TrueCostService {

    private final OfferEligibilityService offerEligibilityService;
    private final PaymentOfferService paymentOfferService;

    @Override
    public CostBreakdownDto calculateTrueCost(NormalizedProductOfferDto offer) {
        if (offer == null) return null;

        BigDecimal basePrice = offer.getPrice() != null ? offer.getPrice() : BigDecimal.ZERO;
        String merchant = offer.getMerchant();
        String category = offer.getCategory();
        String deliveryText = offer.getDelivery();

        // 1. Additional Fees
        BigDecimal deliveryFee = offerEligibilityService.determineDeliveryFee(merchant, deliveryText, basePrice);
        BigDecimal platformFee = offerEligibilityService.determinePlatformFee(merchant);
        BigDecimal otherFees = BigDecimal.ZERO;

        // 2. Applicable Discounts & Bank Offers
        List<AppliedOfferDto> appliedOffers = offerEligibilityService.evaluateOffers(merchant, category, basePrice);

        BigDecimal discounts = BigDecimal.ZERO;
        boolean hasConditional = false;

        for (AppliedOfferDto discountOffer : appliedOffers) {
            if (discountOffer.getDiscountAmount() != null) {
                discounts = discounts.add(discountOffer.getDiscountAmount());
            }
            if (Boolean.TRUE.equals(discountOffer.getIsConditional())) {
                hasConditional = true;
            }
        }

        // 3. True Effective Price Calculation
        // effectivePrice = basePrice + deliveryFee + platformFee + otherFees - discounts
        BigDecimal effectivePrice = basePrice
                .add(deliveryFee)
                .add(platformFee)
                .add(otherFees)
                .subtract(discounts);

        if (effectivePrice.compareTo(BigDecimal.ZERO) < 0) {
            effectivePrice = BigDecimal.ZERO;
        }

        // 4. Payment Offer Optimization
        BigDecimal additionalFees = deliveryFee.add(platformFee).add(otherFees);
        PaymentOfferSummaryDto paymentOffers = paymentOfferService.evaluatePaymentOffers(
                basePrice, additionalFees, merchant, category, null);

        return CostBreakdownDto.builder()
                .basePrice(basePrice)
                .deliveryFee(deliveryFee)
                .platformFee(platformFee)
                .otherFees(otherFees)
                .discounts(discounts)
                .effectivePrice(effectivePrice)
                .currency(offer.getCurrency() != null ? offer.getCurrency() : "INR")
                .appliedOffers(appliedOffers)
                .hasConditionalDiscounts(hasConditional)
                .paymentOffers(paymentOffers)
                .build();
    }

    @Override
    public List<NormalizedProductOfferDto> enrichWithTrueCost(List<NormalizedProductOfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            return new ArrayList<>();
        }

        for (NormalizedProductOfferDto offer : offers) {
            CostBreakdownDto breakdown = calculateTrueCost(offer);
            offer.setCostBreakdown(breakdown);
            offer.setEffectivePrice(breakdown.getEffectivePrice());
        }

        return offers;
    }
}
