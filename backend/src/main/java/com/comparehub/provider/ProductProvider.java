package com.comparehub.provider;

import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface ProductProvider {

    String getProviderName();

    List<NormalizedProductOfferDto> searchProducts(String query);
}
