package com.comparehub.provider;

import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface ProductProvider {

    String getProviderName();

    default List<NormalizedProductOfferDto> searchProducts(String query) {
        return searchProducts(query, 1, 20);
    }

    List<NormalizedProductOfferDto> searchProducts(String query, int page, int pageSize);
}
