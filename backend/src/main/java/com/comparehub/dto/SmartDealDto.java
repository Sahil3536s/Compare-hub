package com.comparehub.dto;

import com.comparehub.model.SmartDealCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartDealDto {

    private String id;
    private String dealType; // "PRODUCT" or "TRAVEL"
    private Long productId;
    private String title;
    private String category;
    private SmartDealCategory dealCategory;
    private String merchantOrProvider;
    private BigDecimal currentPrice;
    private BigDecimal historicalTypicalPrice;
    private BigDecimal originalAdvertisedPrice;
    private BigDecimal realSavingsAmount;
    private int realDiscountPercent;
    private int dealScore;
    private String dealClassification;
    private String dealLabel;
    private String reason;
    private boolean watchlistMatch;
    private boolean alertTriggered;
    private String imageUrl;
    private String linkUrl;
}
