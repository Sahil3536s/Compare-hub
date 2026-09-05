package com.comparehub.service;

import com.comparehub.dto.ProductAttributesDto;

public interface ProductAttributeExtractor {

    ProductAttributesDto extractAttributes(String title, String brandFallback, String categoryFallback);
}
