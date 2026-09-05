package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;

import java.util.List;

public interface ProductMatchingService {

    List<CanonicalProductGroupDto> matchAndGroupOffers(List<NormalizedProductOfferDto> offers);

    List<NormalizedProductOfferDto> enrichWithMatching(List<NormalizedProductOfferDto> offers);
}
