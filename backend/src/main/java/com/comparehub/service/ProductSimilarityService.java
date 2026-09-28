package com.comparehub.service;

import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.dto.ProductSimilarityResultDto;

public interface ProductSimilarityService {

    ProductSimilarityResultDto calculateSimilarity(ProductAttributesDto attrA, ProductAttributesDto attrB, String titleA, String titleB);
}
