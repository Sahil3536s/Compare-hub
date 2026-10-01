package com.comparehub.service;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.model.Product;

import java.math.BigDecimal;

public interface PriceHistoryService {

    void recordPriceIfChanged(Product product, String merchant, BigDecimal price, String currency, String dataSource, Boolean isLive);

    default void recordPriceIfChanged(Product product, String merchant, BigDecimal price, String currency) {
        recordPriceIfChanged(product, merchant, price, currency, "UNKNOWN", false);
    }

    ProductPriceHistoryResponseDto getPriceHistory(Long productId, String period);

    com.comparehub.dto.PriceMeterDto calculatePriceMeter(Long productId, BigDecimal currentPrice, String period);
}
