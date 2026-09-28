package com.comparehub.service.impl;

import com.comparehub.dto.CostBreakdownDto;
import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.EffectiveCostResultDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.PurchaseTimingDto;
import com.comparehub.service.DealQualityService;
import com.comparehub.service.EffectiveCostService;
import com.comparehub.service.ProductAttributeExtractor;
import com.comparehub.service.ProductNormalizationService;
import com.comparehub.service.PurchaseTimingService;
import com.comparehub.service.TrueCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ProductNormalizationServiceImpl implements ProductNormalizationService {

    private final ProductAttributeExtractor attributeExtractor;
    private final EffectiveCostService effectiveCostService;
    private final TrueCostService trueCostService;
    private final DealQualityService dealQualityService;
    private final PurchaseTimingService purchaseTimingService;

    // Overloaded constructor for backwards compatibility with legacy tests
    public ProductNormalizationServiceImpl(
            TrueCostService trueCostService,
            DealQualityService dealQualityService,
            PurchaseTimingService purchaseTimingService) {
        this(new ProductAttributeExtractorImpl(), new EffectiveCostServiceImpl(), trueCostService, dealQualityService, purchaseTimingService);
    }

    @Override
    public NormalizedProductOfferDto normalizeOffer(NormalizedProductOfferDto raw) {
        if (raw == null) return null;

        // 1. Price parsing & normalization
        BigDecimal price = attributeExtractor.normalizePrice(raw.getCurrentPrice() != null ? raw.getCurrentPrice() : raw.getPrice());
        if (price == null) {
            price = BigDecimal.ZERO;
        }

        BigDecimal originalPrice = attributeExtractor.normalizePrice(raw.getOriginalPrice());
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            originalPrice = price;
        }

        // 2. Discount percentage calculation
        Integer discountPercent = 0;
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0 && originalPrice.compareTo(price) > 0) {
            BigDecimal diff = originalPrice.subtract(price);
            discountPercent = diff.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        // 3. Title cleaning (preserves model tokens, removes promo buzzwords)
        String rawTitle = raw.getTitle() != null ? raw.getTitle() : raw.getProductName();
        String cleanTitle = attributeExtractor.cleanTitle(rawTitle);
        if (cleanTitle.isEmpty()) {
            cleanTitle = "Unknown Product";
        }

        // 4. Currency normalization
        String currency = attributeExtractor.normalizeCurrency(raw.getCurrency());

        // 5. Rating: NEVER invent missing values (leave null if unavailable, do NOT default to 4.5)
        Double rating = raw.getRating();

        // 6. Delivery details
        String delivery = raw.getDeliveryEstimate() != null ? raw.getDeliveryEstimate() : raw.getDelivery();
        BigDecimal deliveryCost = attributeExtractor.normalizePrice(raw.getDeliveryCost());

        // 7. InStock / Availability
        Boolean inStock = raw.getInStock() != null ? raw.getInStock()
                : (raw.getAvailability() != null ? raw.getAvailability() : true);

        // 8. Brand normalization
        String brand = attributeExtractor.normalizeBrand(raw.getBrand());

        NormalizedProductOfferDto normalized = NormalizedProductOfferDto.builder()
                .productName(cleanTitle)
                .title(cleanTitle)
                .merchant(raw.getMerchant() != null ? raw.getMerchant().trim() : (raw.getProvider() != null ? raw.getProvider().trim() : "Unknown Merchant"))
                .provider(raw.getProvider() != null ? raw.getProvider().trim() : (raw.getMerchant() != null ? raw.getMerchant().trim() : "Unknown Provider"))
                .externalProductId(raw.getExternalProductId())
                .price(price)
                .currentPrice(price)
                .originalPrice(originalPrice)
                .currency(currency)
                .rating(rating)
                .reviewCount(raw.getReviewCount())
                .delivery(delivery)
                .deliveryEstimate(delivery)
                .deliveryCost(deliveryCost)
                .imageUrl(raw.getImageUrl())
                .productUrl(raw.getProductUrl() != null ? raw.getProductUrl().trim() : "#")
                .inStock(inStock)
                .availability(inStock)
                .discountPercent(discountPercent)
                .discountPercentage(discountPercent)
                .isCheapest(Boolean.TRUE.equals(raw.getIsCheapest()))
                .isBestValue(Boolean.TRUE.equals(raw.getIsBestValue()))
                .isHighestRated(Boolean.TRUE.equals(raw.getIsHighestRated()))
                .brand(brand)
                .category(raw.getCategory() != null ? raw.getCategory().trim() : "General")
                .model(raw.getModel())
                .modelNumber(raw.getModelNumber())
                .ram(raw.getRam())
                .storage(raw.getStorage())
                .color(raw.getColor())
                .variant(raw.getVariant())
                .network(raw.getNetwork())
                .attributes(raw.getAttributes())
                .canonicalKey(raw.getCanonicalKey())
                .build();

        // 9. Attribute extraction (prefers structured attributes over title parsing)
        attributeExtractor.extractAttributes(normalized);

        // 10. Cost Breakdown Calculation
        CostBreakdownDto breakdown = trueCostService.calculateTrueCost(normalized);
        normalized.setCostBreakdown(breakdown);
        if (breakdown != null && breakdown.getPaymentOffers() != null) {
            normalized.setPaymentOffers(breakdown.getPaymentOffers());
            normalized.setBestPaymentPrice(breakdown.getPaymentOffers().getBestEligiblePrice());
        }

        // 11. Calculate Effective Cost according to Part 7 rules
        EffectiveCostResultDto effectiveCostResult = effectiveCostService.calculateEffectiveCost(normalized);
        normalized.setEffectiveCostResult(effectiveCostResult);
        normalized.setEffectiveCost(effectiveCostResult.getEffectiveCost());
        normalized.setEffectivePrice(effectiveCostResult.getEffectiveCost());
        normalized.setConditionalOffers(effectiveCostResult.getConditionalOffers());

        // 12. Calculate Deal Quality
        DealQualityDto dealQuality = dealQualityService.evaluateOfferDealQuality(normalized);
        normalized.setDealQuality(dealQuality);

        // 13. Calculate Purchase Timing
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
