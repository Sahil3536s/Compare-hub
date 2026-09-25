package com.comparehub.service.impl;

import com.comparehub.dto.AppliedOfferDto;
import com.comparehub.dto.ConditionalOfferDto;
import com.comparehub.dto.EffectiveCostResultDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.EffectiveCostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EffectiveCostServiceImpl implements EffectiveCostService {

    private static final Pattern DELIVERY_PRICE_PATTERN = Pattern.compile("(?i)(?:₹|rs\\.?|\\+)\\s*([0-9]+(?:\\.[0-9]{1,2})?)");

    @Override
    public EffectiveCostResultDto calculateEffectiveCost(NormalizedProductOfferDto offer) {
        if (offer == null) {
            return EffectiveCostResultDto.builder()
                    .basePrice(BigDecimal.ZERO)
                    .deliveryCharge(null)
                    .deliveryChargeKnown(false)
                    .mandatoryFee(null)
                    .mandatoryFeeKnown(false)
                    .confirmedDiscount(BigDecimal.ZERO)
                    .effectiveCost(BigDecimal.ZERO)
                    .currency("INR")
                    .build();
        }

        // 1. Base Price
        BigDecimal basePrice = offer.getCurrentPrice() != null ? offer.getCurrentPrice()
                : (offer.getPrice() != null ? offer.getPrice() : BigDecimal.ZERO);
        String currency = offer.getCurrency() != null ? offer.getCurrency() : "INR";

        // 2. Delivery Charge Resolution (Rule: Never invent, never assume 0 if unknown)
        BigDecimal deliveryCharge = null;
        boolean deliveryChargeKnown = false;

        if (offer.getDeliveryCost() != null) {
            deliveryCharge = offer.getDeliveryCost();
            deliveryChargeKnown = true;
        } else if (offer.getDelivery() != null && !offer.getDelivery().isBlank()) {
            String deliv = offer.getDelivery().trim().toLowerCase(Locale.ROOT);
            if (deliv.contains("free") || deliv.equals("0")) {
                deliveryCharge = BigDecimal.ZERO;
                deliveryChargeKnown = true;
            } else {
                Matcher m = DELIVERY_PRICE_PATTERN.matcher(offer.getDelivery());
                if (m.find()) {
                    try {
                        deliveryCharge = new BigDecimal(m.group(1));
                        deliveryChargeKnown = true;
                    } catch (Exception ignored) {
                        deliveryCharge = null;
                        deliveryChargeKnown = false;
                    }
                } else {
                    // Standard delivery without known price -> UNKNOWN
                    deliveryCharge = null;
                    deliveryChargeKnown = false;
                }
            }
        }

        // 3. Mandatory / Platform Fees (Never invent fee)
        BigDecimal mandatoryFee = null;
        boolean mandatoryFeeKnown = false;

        if (offer.getCostBreakdown() != null && offer.getCostBreakdown().getPlatformFee() != null) {
            mandatoryFee = offer.getCostBreakdown().getPlatformFee();
            mandatoryFeeKnown = true;
        }

        // 4. Confirmed Discounts vs Conditional Offers
        BigDecimal confirmedDiscount = BigDecimal.ZERO;
        List<ConditionalOfferDto> conditionalOffers = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // If existing conditional offers attached to offer, retain them
        if (offer.getConditionalOffers() != null) {
            conditionalOffers.addAll(offer.getConditionalOffers());
        }

        // Parse from costBreakdown applied offers if present
        if (offer.getCostBreakdown() != null && offer.getCostBreakdown().getAppliedOffers() != null) {
            for (AppliedOfferDto applied : offer.getCostBreakdown().getAppliedOffers()) {
                if (Boolean.TRUE.equals(applied.getIsConditional())) {
                    boolean alreadyExists = conditionalOffers.stream()
                            .anyMatch(c -> c.getDescription() != null && c.getDescription().equalsIgnoreCase(applied.getDescription()));
                    if (!alreadyExists) {
                        conditionalOffers.add(ConditionalOfferDto.builder()
                                .offerId(applied.getType())
                                .description(applied.getDescription())
                                .discountAmount(applied.getDiscountAmount())
                                .conditionalOffer(true)
                                .requiredBankOrCard(applied.getDescription())
                                .terms(applied.getTerms())
                                .build());
                    }
                } else if (applied.getDiscountAmount() != null) {
                    confirmedDiscount = confirmedDiscount.add(applied.getDiscountAmount());
                }
            }
        }

        // 5. Effective Cost Calculation
        // Effective Cost = Product Price + Known Delivery Charge + Known Mandatory Fee - Confirmed Applicable Discount
        BigDecimal calculatedCost = basePrice;
        if (deliveryChargeKnown && deliveryCharge != null) {
            calculatedCost = calculatedCost.add(deliveryCharge);
        } else {
            notes.add("Delivery charge unknown; not added to effective cost");
        }

        if (mandatoryFeeKnown && mandatoryFee != null) {
            calculatedCost = calculatedCost.add(mandatoryFee);
        } else {
            notes.add("Mandatory fee unknown or not charged");
        }

        if (confirmedDiscount.compareTo(BigDecimal.ZERO) > 0) {
            calculatedCost = calculatedCost.subtract(confirmedDiscount);
            notes.add("Applied confirmed unconditional discount of " + currency + " " + confirmedDiscount);
        }

        if (calculatedCost.compareTo(BigDecimal.ZERO) < 0) {
            calculatedCost = BigDecimal.ZERO;
        }

        if (!conditionalOffers.isEmpty()) {
            notes.add(conditionalOffers.size() + " conditional payment/bank offer(s) available; kept separate from unconditional effective cost");
        }

        return EffectiveCostResultDto.builder()
                .basePrice(basePrice)
                .deliveryCharge(deliveryCharge)
                .deliveryChargeKnown(deliveryChargeKnown)
                .mandatoryFee(mandatoryFee)
                .mandatoryFeeKnown(mandatoryFeeKnown)
                .confirmedDiscount(confirmedDiscount)
                .effectiveCost(calculatedCost)
                .currency(currency)
                .conditionalOffers(conditionalOffers)
                .calculationNotes(notes)
                .build();
    }
}
