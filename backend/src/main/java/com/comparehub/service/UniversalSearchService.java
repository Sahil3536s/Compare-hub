package com.comparehub.service;

import com.comparehub.dto.UniversalSearchRequestDto;
import com.comparehub.dto.UniversalSearchResponseDto;

public interface UniversalSearchService {

    UniversalSearchResponseDto executeUniversalSearch(UniversalSearchRequestDto request);
}
