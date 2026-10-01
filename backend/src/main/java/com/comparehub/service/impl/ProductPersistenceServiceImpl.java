package com.comparehub.service.impl;

import com.comparehub.dto.CanonicalProductGroupDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.model.MerchantOffer;
import com.comparehub.model.Product;
import com.comparehub.repository.MerchantOfferRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.PriceHistoryService;
import com.comparehub.service.ProductPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPersistenceServiceImpl implements ProductPersistenceService {

    private final ProductRepository productRepository;
    private final MerchantOfferRepository merchantOfferRepository;
    private final PriceHistoryService priceHistoryService;

    @Override
    @Transactional
    public void persistCanonicalGroupsAndOffers(List<CanonicalProductGroupDto> canonicalGroups) {
        if (canonicalGroups == null || canonicalGroups.isEmpty()) {
            return;
        }

        for (CanonicalProductGroupDto group : canonicalGroups) {
            try {
                Product product = findOrCreateProduct(group);
                group.setProductId(product.getId());

                if (group.getOffers() != null) {
                    List<MerchantOffer> existingOffers = merchantOfferRepository.findByProductIdOrderByPriceAsc(product.getId());

                    for (NormalizedProductOfferDto offerDto : group.getOffers()) {
                        offerDto.setProductId(product.getId());

                        // Match or create MerchantOffer in database
                        MerchantOffer targetOffer = existingOffers.stream()
                                .filter(o -> o.getMerchant().equalsIgnoreCase(offerDto.getMerchant()))
                                .findFirst()
                                .orElse(null);

                        BigDecimal price = offerDto.getPrice() != null ? offerDto.getPrice() : BigDecimal.ZERO;
                        BigDecimal rating = offerDto.getRating() != null ? BigDecimal.valueOf(offerDto.getRating()) : null;
                        String url = offerDto.getProductUrl() != null && !offerDto.getProductUrl().isBlank()
                                ? offerDto.getProductUrl()
                                : "https://www.google.com/search?q=" + product.getName().replace(" ", "+");

                        if (targetOffer == null) {
                            targetOffer = MerchantOffer.builder()
                                    .product(product)
                                    .merchant(offerDto.getMerchant())
                                    .price(price)
                                    .originalPrice(offerDto.getOriginalPrice())
                                    .productUrl(url.length() > 2000 ? url.substring(0, 2000) : url)
                                    .inStock(offerDto.getInStock() != null ? offerDto.getInStock() : true)
                                    .rating(rating)
                                    .deliveryText(offerDto.getDeliveryText())
                                    .lastUpdated(Instant.now())
                                    .build();
                        } else {
                            targetOffer.setPrice(price);
                            targetOffer.setOriginalPrice(offerDto.getOriginalPrice());
                            targetOffer.setProductUrl(url.length() > 2000 ? url.substring(0, 2000) : url);
                            targetOffer.setInStock(offerDto.getInStock() != null ? offerDto.getInStock() : true);
                            if (rating != null) {
                                targetOffer.setRating(rating);
                            }
                            if (offerDto.getDeliveryText() != null) {
                                targetOffer.setDeliveryText(offerDto.getDeliveryText());
                            }
                            targetOffer.setLastUpdated(Instant.now());
                        }

                        merchantOfferRepository.save(targetOffer);

                        // Strictly exclude demo data: only record price history for authentic LIVE offers
                        boolean isDemo = !offerDto.isLive() || "DEMO".equalsIgnoreCase(offerDto.getDataSource());

                        if (!isDemo && price.compareTo(BigDecimal.ZERO) > 0) {
                            String currency = offerDto.getCurrency() != null ? offerDto.getCurrency() : "INR";
                            String dataSource = offerDto.getDataSource() != null ? offerDto.getDataSource() : "LIVE";
                            boolean live = offerDto.getLive() != null ? offerDto.getLive() : true;
                            priceHistoryService.recordPriceIfChanged(product, offerDto.getMerchant(), price, currency, dataSource, live);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to persist canonical group {}: {}", group.getCanonicalTitle(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public Product findOrCreateProduct(CanonicalProductGroupDto group) {
        String title = group.getCanonicalTitle();
        if (title == null || title.isBlank()) {
            title = "Unknown Product";
        }
        if (title.length() > 255) {
            title = title.substring(0, 255);
        }

        Optional<Product> existingOpt = productRepository.findFirstByNameIgnoreCase(title);
        if (existingOpt.isPresent()) {
            Product existing = existingOpt.get();
            if ((existing.getImageUrl() == null || existing.getImageUrl().isBlank()) && group.getImageUrl() != null) {
                existing.setImageUrl(group.getImageUrl());
                productRepository.save(existing);
            }
            return existing;
        }

        String brand = group.getBrand();
        if (brand == null && group.getAttributes() != null) {
            brand = group.getAttributes().getBrand();
        }
        if (brand != null && brand.length() > 100) {
            brand = brand.substring(0, 100);
        }

        String category = group.getCategory();
        if (category == null || category.isBlank()) {
            category = "Electronics";
        }
        if (category.length() > 100) {
            category = category.substring(0, 100);
        }

        String imageUrl = group.getImageUrl();
        if (imageUrl != null && imageUrl.length() > 1000) {
            imageUrl = imageUrl.substring(0, 1000);
        }

        Product newProduct = Product.builder()
                .name(title)
                .brand(brand)
                .category(category)
                .imageUrl(imageUrl)
                .build();

        return productRepository.save(newProduct);
    }
}
