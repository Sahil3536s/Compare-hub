package com.comparehub.service;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.model.Product;

import java.util.List;

public interface ProductPersistenceService {

    /**
     * Resolves or saves canonical products and their merchant offers in PostgreSQL.
     * Records real price history observations for live/authentic offers.
     * Populates generated product IDs back into CanonicalProductGroupDto and NormalizedProductOfferDto.
     */
    void persistCanonicalGroupsAndOffers(List<CanonicalProductGroupDto> canonicalGroups);

    /**
     * Finds a Product by canonical key or by ID.
     */
    Product findOrCreateProduct(CanonicalProductGroupDto group);
}
