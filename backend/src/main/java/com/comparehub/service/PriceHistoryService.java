package com.comparehub.service;

import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.model.Product;

import java.math.BigDecimal;

public interface PriceHistoryService {

    void recordPriceIfChanged(Product product, String merchant, BigDecimal price, String currency);

    ProductPriceHistoryResponseDto getPriceHistory(Long productId, String period);
}
