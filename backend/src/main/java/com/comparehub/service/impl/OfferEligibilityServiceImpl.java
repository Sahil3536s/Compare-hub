package com.comparehub.service.impl;

import com.comparehub.dto.AppliedOfferDto;
import com.comparehub.service.OfferEligibilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OfferEligibilityServiceImpl implements OfferEligibilityService {

    private static final Pattern DELIVERY_PRICE_PATTERN = Pattern.compile(
            "(?:₹|rs\\.?)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<AppliedOfferDto> evaluateOffers(String merchant, String category, BigDecimal basePrice) {
        List<AppliedOfferDto> offers = new ArrayList<>();
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return offers;
        }

        String m = merchant != null ? merchant.trim().toLowerCase() : "";

        if (m.contains("amazon")) {
            if (basePrice.compareTo(BigDecimal.valueOf(50000)) >= 0) {
                offers.add(AppliedOfferDto.builder()
                        .type("PAYMENT_OFFER")
                        .description("₹1,500 Instant Discount on HDFC & ICICI Credit Cards")
                        .discountAmount(BigDecimal.valueOf(1500))
                        .isConditional(true)
                        .terms("Requires eligible HDFC or ICICI Bank Credit Card transaction")
                        .build());
            } else if (basePrice.compareTo(BigDecimal.valueOf(20000)) >= 0) {
                offers.add(AppliedOfferDto.builder()
                        .type("PAYMENT_OFFER")
                        .description("₹1,000 Instant Discount on SBI Credit Cards")
                        .discountAmount(BigDecimal.valueOf(1000))
                        .isConditional(true)
                        .terms("Valid on minimum purchase of ₹20,000 via SBI Card")
                        .build());
            }
        } else if (m.contains("flipkart")) {
            if (basePrice.compareTo(BigDecimal.valueOf(30000)) >= 0) {
                offers.add(AppliedOfferDto.builder()
                        .type("PAYMENT_OFFER")
                        .description("₹1,250 Off with Axis Bank Credit Card")
                        .discountAmount(BigDecimal.valueOf(1250))
                        .isConditional(true)
                        .terms("Applicable on Flipkart Axis Bank Credit Card")
                        .build());
            } else if (basePrice.compareTo(BigDecimal.valueOf(10000)) >= 0) {
                offers.add(AppliedOfferDto.builder()
                        .type("PAYMENT_OFFER")
                        .description("₹750 Instant Discount on ICICI Bank Cards")
                        .discountAmount(BigDecimal.valueOf(750))
                        .isConditional(true)
                        .terms("Applicable on ICICI Credit & Debit Cards")
                        .build());
            }
        } else if (m.contains("croma")) {
            if (basePrice.compareTo(BigDecimal.valueOf(20000)) >= 0) {
                offers.add(AppliedOfferDto.builder()
                        .type("PAYMENT_OFFER")
                        .description("₹1,000 Instant Discount on Tata Neu HDFC Cards")
                        .discountAmount(BigDecimal.valueOf(1000))
                        .isConditional(true)
                        .terms("Valid at checkout on Croma.com")
                        .build());
            }
        }

        return offers;
    }

    @Override
    public BigDecimal determineDeliveryFee(String merchant, String deliveryText, BigDecimal basePrice) {
        if (deliveryText != null && deliveryText.toLowerCase().contains("free")) {
            return BigDecimal.ZERO;
        }

        if (deliveryText != null) {
            Matcher m = DELIVERY_PRICE_PATTERN.matcher(deliveryText);
            if (m.find()) {
                try {
                    return new BigDecimal(m.group(1));
                } catch (Exception ignored) {}
            }
        }

        String m = merchant != null ? merchant.toLowerCase() : "";
        if (m.contains("amazon") || m.contains("flipkart")) {
            // Free for orders >= 500
            if (basePrice != null && basePrice.compareTo(BigDecimal.valueOf(500)) >= 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(40);
        } else if (m.contains("croma")) {
            if (basePrice != null && basePrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(99);
        }

        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal determinePlatformFee(String merchant) {
        if (merchant == null) return BigDecimal.ZERO;
        String m = merchant.toLowerCase();
        if (m.contains("flipkart")) {
            return BigDecimal.valueOf(3); // ₹3 standard platform fee
        }
        return BigDecimal.ZERO;
    }
}
