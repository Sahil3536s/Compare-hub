package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductMatchResultDto;

import java.util.List;

public interface ProductMatchingService {

    ProductMatchResultDto evaluateMatch(NormalizedProductOfferDto a, NormalizedProductOfferDto b);

    List<CanonicalProductGroupDto> matchAndGroupOffers(List<NormalizedProductOfferDto> offers);

    List<NormalizedProductOfferDto> enrichWithMatching(List<NormalizedProductOfferDto> offers);
}
