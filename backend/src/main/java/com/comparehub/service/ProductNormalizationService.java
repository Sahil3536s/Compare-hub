package com.comparehub.service;

import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface ProductNormalizationService {

    NormalizedProductOfferDto normalizeOffer(NormalizedProductOfferDto rawOffer);

    List<NormalizedProductOfferDto> normalizeOffers(List<NormalizedProductOfferDto> rawOffers);
}
