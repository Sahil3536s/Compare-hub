package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedProductResponseDto {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productBrand;
    private String productCategory;
    private String productImageUrl;
    private BigDecimal savedPrice;
    private String savedMerchant;
    private BigDecimal currentPrice;
    private String currentMerchant;
    private BigDecimal priceChange;
    private BigDecimal priceDropAmount;
    private Double priceDropPercentage;
    private Boolean isPriceDropped;
    private Boolean hasActiveAlert;
    private BigDecimal alertTargetPrice;
    private Long alertId;
    private String productUrl;
    private Instant createdAt;
}
