package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalProductDetailDto {

    private Long id;
    private String canonicalKey;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private Double rating;
    private Integer reviewCount;

    // Pricing summary
    private BigDecimal bestCurrentPrice;
    private String bestMerchant;
    private BigDecimal highestCurrentPrice;
    private BigDecimal historicalAveragePrice;
    private Double diffPercentFromAverage;

    // Attributes / Specifications (generic and flexible)
    private ProductAttributesDto structuredAttributes;
    @Builder.Default
    private Map<String, String> specifications = new LinkedHashMap<>();

    // Merchant comparison offers
    @Builder.Default
    private List<NormalizedProductOfferDto> merchantOffers = new ArrayList<>();

    // Statistical Price Meter
    private PriceMeterDto priceMeter;

    // Price History (90D default)
    private ProductPriceHistoryResponseDto priceHistory;

    // ML Price Prediction (distinguished from statistical price meter)
    private PricePredictionResponseDto mlPrediction;

    // Alternative / Similar Products
    @Builder.Default
    private List<ProductAlternativeDto> alternatives = new ArrayList<>();

    public BigDecimal getCurrentLowestPrice() {
        return bestCurrentPrice;
    }

    public void setCurrentLowestPrice(BigDecimal p) {
        this.bestCurrentPrice = p;
    }

    public String getCheapestMerchant() {
        return bestMerchant;
    }

    public void setCheapestMerchant(String m) {
        this.bestMerchant = m;
    }

    public List<NormalizedProductOfferDto> getOffers() {
        return merchantOffers;
    }

    public void setOffers(List<NormalizedProductOfferDto> o) {
        this.merchantOffers = o;
    }
}
