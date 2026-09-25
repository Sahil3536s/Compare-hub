package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductAttributesDto;

import java.math.BigDecimal;

public interface ProductAttributeExtractor {

    ProductAttributesDto extractAttributes(String title, String brandFallback, String categoryFallback);

    ProductAttributesDto extractAttributes(NormalizedProductOfferDto offer);

    BigDecimal normalizePrice(Object rawPrice);

    String normalizeCurrency(String rawCurrency);

    String normalizeBrand(String rawBrand);

    String normalizeStorage(String rawStorage);

    String normalizeRam(String rawRam);

    String extractNetwork(String text);

    String cleanTitle(String rawTitle);
}
