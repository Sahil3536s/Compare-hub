package com.comparehub.service.impl;

import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.PurchaseTimingDto;
import com.comparehub.service.DealQualityService;
import com.comparehub.service.ProductNormalizationService;
import com.comparehub.service.PurchaseTimingService;
import com.comparehub.service.TrueCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductNormalizationServiceImpl implements ProductNormalizationService {

    private final TrueCostService trueCostService;
    private final DealQualityService dealQualityService;
    private final PurchaseTimingService purchaseTimingService;

    @Override
    public NormalizedProductOfferDto normalizeOffer(NormalizedProductOfferDto raw) {
        if (raw == null) return null;

        BigDecimal price = raw.getPrice() != null ? raw.getPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = raw.getOriginalPrice() != null ? raw.getOriginalPrice() : price;

        Integer discountPercent = 0;
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0 && originalPrice.compareTo(price) > 0) {
            BigDecimal diff = originalPrice.subtract(price);
            discountPercent = diff.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        NormalizedProductOfferDto normalized = NormalizedProductOfferDto.builder()
                .productName(raw.getProductName() != null ? raw.getProductName().trim() : "Unknown Product")
                .merchant(raw.getMerchant() != null ? raw.getMerchant().trim() : "Unknown Merchant")
                .price(price)
                .originalPrice(originalPrice)
                .currency(raw.getCurrency() != null ? raw.getCurrency().trim() : "INR")
                .rating(raw.getRating() != null ? raw.getRating() : 4.5)
                .delivery(raw.getDelivery() != null ? raw.getDelivery().trim() : "Standard Delivery")
                .imageUrl(raw.getImageUrl())
                .productUrl(raw.getProductUrl() != null ? raw.getProductUrl().trim() : "#")
                .inStock(raw.getInStock() != null ? raw.getInStock() : true)
                .discountPercent(discountPercent)
                .isCheapest(raw.getIsCheapest() != null ? raw.getIsCheapest() : false)
                .isBestValue(raw.getIsBestValue() != null ? raw.getIsBestValue() : false)
                .isHighestRated(raw.getIsHighestRated() != null ? raw.getIsHighestRated() : false)
                .brand(raw.getBrand() != null ? raw.getBrand().trim() : "Generic")
                .category(raw.getCategory() != null ? raw.getCategory().trim() : "General")
                .attributes(raw.getAttributes())
                .canonicalKey(raw.getCanonicalKey())
                .build();

        // Calculate True Effective Cost Breakdown
        CostBreakdownDto breakdown = trueCostService.calculateTrueCost(normalized);
        normalized.setCostBreakdown(breakdown);
        normalized.setEffectivePrice(breakdown != null ? breakdown.getEffectivePrice() : price);
        if (breakdown != null && breakdown.getPaymentOffers() != null) {
            normalized.setPaymentOffers(breakdown.getPaymentOffers());
            normalized.setBestPaymentPrice(breakdown.getPaymentOffers().getBestEligiblePrice());
        }

        // Calculate Deal Quality
        DealQualityDto dealQuality = dealQualityService.evaluateOfferDealQuality(normalized);
        normalized.setDealQuality(dealQuality);

        // Calculate Purchase Timing
        PurchaseTimingDto purchaseTiming = purchaseTimingService.evaluateOfferTiming(normalized);
        normalized.setPurchaseTiming(purchaseTiming);

        return normalized;
    }

    @Override
    public List<NormalizedProductOfferDto> normalizeOffers(List<NormalizedProductOfferDto> rawOffers) {
        if (rawOffers == null) return new ArrayList<>();
        return rawOffers.stream()
                .map(this::normalizeOffer)
                .collect(Collectors.toList());
    }
}
