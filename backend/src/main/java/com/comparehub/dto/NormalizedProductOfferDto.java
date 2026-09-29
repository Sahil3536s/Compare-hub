package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedProductOfferDto {

    private Long productId;
    private String productName;
    private String title;
    private String merchant;
    private String provider;
    private String externalProductId;
    @Builder.Default
    private String dataSource = "LIVE"; // LIVE or DEMO
    @Builder.Default
    private Boolean live = true;
    private BigDecimal price; // Listed / Base Price
    private BigDecimal currentPrice;
    private BigDecimal originalPrice;
    @Builder.Default
    private String currency = "INR";
    private Double rating;
    private Integer reviewCount;
    private String delivery;
    private String deliveryEstimate;
    private BigDecimal deliveryCost;
    private String imageUrl;
    private String productUrl;
    @Builder.Default
    private Boolean inStock = true;
    @Builder.Default
    private Boolean availability = true;
    private Integer discountPercent;
    private Integer discountPercentage;
    @Builder.Default
    private Boolean isCheapest = false;
    @Builder.Default
    private Boolean isBestValue = false;
    @Builder.Default
    private Boolean isHighestRated = false;
    @Builder.Default
    private Boolean isFastestDelivery = false;
    private Double rankingScore;
    private String brand;
    private String model;
    private String modelNumber;
    private String ram;
    private String storage;
    private String color;
    private String variant;
    private String network;
    private String category;
    private java.time.Instant lastUpdated;

    // Phase 24: Universal Generic Attributes & Intelligent Matching
    @Builder.Default
    private java.util.Map<String, String> attributes = new java.util.LinkedHashMap<>();
    private ProductAttributesDto structuredAttributes;
    private String subcategory;
    private Double relevanceScore;
    private String canonicalKey;

    // Phase 25: True Cost & Effective Cost Engine
    private BigDecimal effectivePrice;
    private BigDecimal effectiveCost;
    private CostBreakdownDto costBreakdown;
    private EffectiveCostResultDto effectiveCostResult;
    @Builder.Default
    private java.util.List<ConditionalOfferDto> conditionalOffers = new java.util.ArrayList<>();

    // Phase 28: Deal Quality Engine
    private DealQualityDto dealQuality;

    // Phase 29: Purchase Timing Engine
    private PurchaseTimingDto purchaseTiming;

    // Phase 30: Payment Offer Optimization
    private PaymentOfferSummaryDto paymentOffers;
    private BigDecimal bestPaymentPrice;

    // Part 8: Multi-Factor Ranking & Explainable Recommendation
    private RankResultDto rankResult;
    private Integer rank;
    private String rankingLabel;
    private String recommendationReason;
    @Builder.Default
    private java.util.List<String> whyThisOption = new java.util.ArrayList<>();
    private Double priceScore;
    private Double ratingScore;
    private Double deliveryScore;
    private Double discountScore;
    private Double availabilityScore;
    private Double finalScore;

    // Alias accessors for Part 7 consistency
    public String getTitle() {
        return title != null && !title.isBlank() ? title : productName;
    }

    public void setTitle(String t) {
        this.title = t;
        if (this.productName == null) this.productName = t;
    }

    public String getProductName() {
        return productName != null && !productName.isBlank() ? productName : title;
    }

    public void setProductName(String p) {
        this.productName = p;
        if (this.title == null) this.title = p;
    }

    public String getProvider() {
        return provider != null && !provider.isBlank() ? provider : merchant;
    }

    public void setProvider(String p) {
        this.provider = p;
        if (this.merchant == null) this.merchant = p;
    }

    public String getMerchant() {
        return merchant != null && !merchant.isBlank() ? merchant : provider;
    }

    public void setMerchant(String m) {
        this.merchant = m;
        if (this.provider == null) this.provider = m;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice != null ? currentPrice : price;
    }

    public void setCurrentPrice(BigDecimal cp) {
        this.currentPrice = cp;
        if (this.price == null) this.price = cp;
    }

    public BigDecimal getPrice() {
        return price != null ? price : currentPrice;
    }

    public void setPrice(BigDecimal p) {
        this.price = p;
        if (this.currentPrice == null) this.currentPrice = p;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage != null ? discountPercentage : discountPercent;
    }

    public void setDiscountPercentage(Integer d) {
        this.discountPercentage = d;
        if (this.discountPercent == null) this.discountPercent = d;
    }

    public Integer getDiscountPercent() {
        return discountPercent != null ? discountPercent : discountPercentage;
    }

    public void setDiscountPercent(Integer d) {
        this.discountPercent = d;
        if (this.discountPercentage == null) this.discountPercentage = d;
    }

    public String getDeliveryEstimate() {
        return deliveryEstimate != null && !deliveryEstimate.isBlank() ? deliveryEstimate : delivery;
    }

    public void setDeliveryEstimate(String d) {
        this.deliveryEstimate = d;
        if (this.delivery == null) this.delivery = d;
    }

    public String getDelivery() {
        return delivery != null && !delivery.isBlank() ? delivery : deliveryEstimate;
    }

    public void setDelivery(String d) {
        this.delivery = d;
        if (this.deliveryEstimate == null) this.deliveryEstimate = d;
    }

    public String getDeliveryText() {
        return delivery != null && !delivery.isBlank() ? delivery : deliveryEstimate;
    }

    public void setDeliveryText(String dt) {
        this.delivery = dt;
        if (this.deliveryEstimate == null) this.deliveryEstimate = dt;
    }

    public boolean isLive() {
        return Boolean.TRUE.equals(this.live);
    }

    public Boolean getAvailability() {
        return availability != null ? availability : inStock;
    }

    public void setAvailability(Boolean a) {
        this.availability = a;
        if (this.inStock == null) this.inStock = a;
    }

    public Boolean getInStock() {
        return inStock != null ? inStock : availability;
    }

    public void setInStock(Boolean s) {
        this.inStock = s;
        if (this.availability == null) this.availability = s;
    }

    public BigDecimal getEffectiveCost() {
        return effectiveCost != null ? effectiveCost : effectivePrice;
    }

    public void setEffectiveCost(BigDecimal ec) {
        this.effectiveCost = ec;
        if (this.effectivePrice == null) this.effectivePrice = ec;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice != null ? effectivePrice : effectiveCost;
    }

    public void setEffectivePrice(BigDecimal ep) {
        this.effectivePrice = ep;
        if (this.effectiveCost == null) this.effectiveCost = ep;
    }

    public String getAttributeValue(String key) {
        if (attributes == null || key == null) return null;
        for (java.util.Map.Entry<String, String> entry : attributes.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void addAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new java.util.LinkedHashMap<>();
        }
        if (key != null && value != null) {
            attributes.put(key, value);
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public ProductAttributesDto getStructuredAttributes() {
        if (structuredAttributes == null && attributes != null && !attributes.isEmpty()) {
            structuredAttributes = ProductAttributesDto.builder()
                    .brand(brand != null ? brand : getAttributeValue("Brand"))
                    .model(model != null ? model : getAttributeValue("Model"))
                    .variant(variant != null ? variant : getAttributeValue("Variant"))
                    .ram(ram != null ? ram : (getAttributeValue("RAM") != null ? getAttributeValue("RAM") : getAttributeValue("ram")))
                    .storage(storage != null ? storage : (getAttributeValue("Storage") != null ? getAttributeValue("Storage") : getAttributeValue("storage")))
                    .color(color != null ? color : (getAttributeValue("Color") != null ? getAttributeValue("Color") : getAttributeValue("color")))
                    .modelNumber(modelNumber != null ? modelNumber : (getAttributeValue("Model Number") != null ? getAttributeValue("Model Number") : getAttributeValue("SKU")))
                    .genericAttributes(attributes)
                    .build();
        }
        return structuredAttributes;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setStructuredAttributes(ProductAttributesDto attr) {
        this.structuredAttributes = attr;
        if (attr != null) {
            if (this.attributes == null) {
                this.attributes = new java.util.LinkedHashMap<>();
            }
            this.attributes.putAll(attr.toMap());
        }
    }

    public void setAttributes(java.util.Map<String, String> attrs) {
        this.attributes = attrs != null ? new java.util.LinkedHashMap<>(attrs) : new java.util.LinkedHashMap<>();
    }

    public static class NormalizedProductOfferDtoBuilder {
        private java.util.Map<String, String> attributes;
        private ProductAttributesDto structuredAttributes;

        public NormalizedProductOfferDtoBuilder attributes(ProductAttributesDto attr) {
            this.structuredAttributes = attr;
            if (attr != null) {
                this.attributes = attr.toMap();
            }
            return this;
        }

        public NormalizedProductOfferDtoBuilder attributes(java.util.Map<String, String> attrs) {
            this.attributes = attrs != null ? new java.util.LinkedHashMap<>(attrs) : new java.util.LinkedHashMap<>();
            return this;
        }

        public NormalizedProductOfferDtoBuilder structuredAttributes(ProductAttributesDto attr) {
            this.structuredAttributes = attr;
            if (attr != null) {
                this.attributes = attr.toMap();
            }
            return this;
        }
    }
}
