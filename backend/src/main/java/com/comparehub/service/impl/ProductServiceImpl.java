package com.comparehub.service.impl;

import com.comparehub.dto.MerchantOfferRequestDto;
import com.comparehub.dto.MerchantOfferResponseDto;
import com.comparehub.dto.ProductCreateRequestDto;
import com.comparehub.dto.ProductResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.MerchantOffer;
import com.comparehub.model.Product;
import com.comparehub.repository.MerchantOfferRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantOfferRepository merchantOfferRepository;
    private final com.comparehub.service.PriceHistoryService priceHistoryService;
    private final com.comparehub.service.ProductAttributeExtractor attributeExtractor;
    private final com.comparehub.service.ProductAlternativeService alternativeService;
    private final com.comparehub.service.PricePredictionService pricePredictionService;

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "product-comparisons", allEntries = true)
    public ProductResponseDto createProduct(ProductCreateRequestDto request) {
        Product product = Product.builder()
                .name(request.getName().trim())
                .brand(request.getBrand() != null ? request.getBrand().trim() : null)
                .category(request.getCategory().trim())
                .imageUrl(request.getImageUrl())
                .offers(new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);

        if (request.getOffers() != null && !request.getOffers().isEmpty()) {
            for (MerchantOfferRequestDto offerDto : request.getOffers()) {
                MerchantOffer offer = MerchantOffer.builder()
                        .product(savedProduct)
                        .merchant(offerDto.getMerchant().trim())
                        .price(offerDto.getPrice())
                        .originalPrice(offerDto.getOriginalPrice())
                        .productUrl(offerDto.getProductUrl())
                        .inStock(offerDto.getInStock() != null ? offerDto.getInStock() : true)
                        .rating(offerDto.getRating())
                        .deliveryText(offerDto.getDeliveryText())
                        .build();
                MerchantOffer savedOffer = merchantOfferRepository.save(offer);
                savedProduct.getOffers().add(savedOffer);
                priceHistoryService.recordPriceIfChanged(savedProduct, savedOffer.getMerchant(), savedOffer.getPrice(), "INR");
            }
        }

        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findByIdWithOffers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public com.comparehub.dto.CanonicalProductDetailDto getCanonicalProductDetail(Long id) {
        Product product = productRepository.findByIdWithOffers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // 1. Extract generic & structured attributes
        com.comparehub.dto.ProductAttributesDto structuredAttrs =
                attributeExtractor.extractAttributes(product.getName(), product.getBrand(), product.getCategory());

        Map<String, String> specs = new LinkedHashMap<>();
        if (product.getBrand() != null && !product.getBrand().isBlank()) specs.put("Brand", product.getBrand());
        if (product.getCategory() != null && !product.getCategory().isBlank()) specs.put("Category", product.getCategory());
        if (structuredAttrs != null) {
            specs.putAll(structuredAttrs.toMap());
        }

        // 2. Map merchant offers into NormalizedProductOfferDto
        List<MerchantOffer> offersList = product.getOffers();
        if (offersList == null || offersList.isEmpty()) {
            offersList = merchantOfferRepository.findByProductIdOrderByPriceAsc(id);
        }
        List<com.comparehub.dto.NormalizedProductOfferDto> normOffers = new ArrayList<>();
        if (offersList != null) {
            for (MerchantOffer off : offersList) {
                com.comparehub.dto.NormalizedProductOfferDto dto = com.comparehub.dto.NormalizedProductOfferDto.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .title(product.getName())
                        .merchant(off.getMerchant())
                        .provider(off.getMerchant())
                        .price(off.getPrice())
                        .currentPrice(off.getPrice())
                        .originalPrice(off.getOriginalPrice())
                        .currency("INR")
                        .productUrl(off.getProductUrl())
                        .inStock(off.getInStock())
                        .availability(off.getInStock())
                        .rating(off.getRating() != null ? off.getRating().doubleValue() : null)
                        .delivery(off.getDeliveryText())
                        .deliveryEstimate(off.getDeliveryText())
                        .imageUrl(product.getImageUrl())
                        .brand(product.getBrand())
                        .category(product.getCategory())
                        .canonicalKey(structuredAttrs != null ? structuredAttrs.getCanonicalKey() : null)
                        .attributes(specs)
                        .dataSource("LIVE")
                        .live(true)
                        .build();
                normOffers.add(dto);
            }
        }

        // Sort cheapest first by default
        normOffers.sort(Comparator.comparing(com.comparehub.dto.NormalizedProductOfferDto::getPrice));

        // Mark cheapest
        if (!normOffers.isEmpty()) {
            normOffers.get(0).setIsCheapest(true);
        }

        BigDecimal bestPrice = !normOffers.isEmpty() ? normOffers.get(0).getPrice() : null;
        String bestMerchant = !normOffers.isEmpty() ? normOffers.get(0).getMerchant() : null;
        BigDecimal highestPrice = !normOffers.isEmpty() ? normOffers.get(normOffers.size() - 1).getPrice() : null;

        // 3. Price History (90D default)
        com.comparehub.dto.ProductPriceHistoryResponseDto priceHistory = null;
        try {
            priceHistory = priceHistoryService.getPriceHistory(id, "90D");
        } catch (Exception e) {
            log.warn("Failed to retrieve price history for product {}: {}", id, e.getMessage());
        }

        // 4. Price Meter Calculation
        com.comparehub.dto.PriceMeterDto priceMeter = null;
        try {
            priceMeter = priceHistoryService.calculatePriceMeter(id, bestPrice, "30D");
        } catch (Exception e) {
            log.warn("Failed to calculate price meter for product {}: {}", id, e.getMessage());
        }

        // 5. ML Price Prediction (Optional, non-blocking)
        com.comparehub.dto.PricePredictionResponseDto mlPred = null;
        try {
            mlPred = pricePredictionService.getPricePrediction(id);
        } catch (Exception e) {
            log.debug("ML price prediction unavailable for product {}: {}", id, e.getMessage());
        }

        // 6. Alternatives discovery
        List<com.comparehub.dto.ProductAlternativeDto> alternatives = new ArrayList<>();
        try {
            com.comparehub.dto.ProductAlternativesResponseDto altResp = alternativeService.getAlternatives(
                    product.getName(), bestPrice, product.getCategory(), product.getBrand(), 4);
            if (altResp != null && altResp.getAlternatives() != null) {
                alternatives = altResp.getAlternatives();
            }
        } catch (Exception e) {
            log.debug("Alternatives discovery error for product {}: {}", id, e.getMessage());
        }

        // Ratings
        Double avgRating = normOffers.stream()
                .filter(o -> o.getRating() != null)
                .mapToDouble(com.comparehub.dto.NormalizedProductOfferDto::getRating)
                .average()
                .orElse(4.6);
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        return com.comparehub.dto.CanonicalProductDetailDto.builder()
                .id(product.getId())
                .canonicalKey(structuredAttrs != null ? structuredAttrs.getCanonicalKey() : null)
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .rating(avgRating)
                .reviewCount(142)
                .bestCurrentPrice(bestPrice)
                .bestMerchant(bestMerchant)
                .highestCurrentPrice(highestPrice)
                .historicalAveragePrice(priceMeter != null ? priceMeter.getHistoricalAverage() : null)
                .diffPercentFromAverage(priceMeter != null ? priceMeter.getPercentDifferenceFromAverage() : null)
                .structuredAttributes(structuredAttrs)
                .specifications(specs)
                .merchantOffers(normOffers)
                .priceMeter(priceMeter)
                .priceHistory(priceHistory)
                .mlPrediction(mlPred)
                .alternatives(alternatives)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAllWithOffers().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProductResponseDto> getAllProducts(org.springframework.data.domain.Pageable pageable) {
        return productRepository.findAllWithOffers(pageable)
                .map(this::mapToProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "product-comparisons", allEntries = true)
    public MerchantOfferResponseDto addMerchantOffer(Long productId, MerchantOfferRequestDto offerRequest) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        MerchantOffer offer = MerchantOffer.builder()
                .product(product)
                .merchant(offerRequest.getMerchant().trim())
                .price(offerRequest.getPrice())
                .originalPrice(offerRequest.getOriginalPrice())
                .productUrl(offerRequest.getProductUrl())
                .inStock(offerRequest.getInStock() != null ? offerRequest.getInStock() : true)
                .rating(offerRequest.getRating())
                .deliveryText(offerRequest.getDeliveryText())
                .build();

        MerchantOffer savedOffer = merchantOfferRepository.save(offer);
        priceHistoryService.recordPriceIfChanged(product, savedOffer.getMerchant(), savedOffer.getPrice(), "INR");
        return mapToOfferResponse(savedOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantOfferResponseDto> getProductOffers(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return merchantOfferRepository.findByProductIdOrderByPriceAsc(productId).stream()
                .map(this::mapToOfferResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "product-comparisons", allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDto mapToProductResponse(Product product) {
        List<MerchantOfferResponseDto> offerDtos = product.getOffers() != null
                ? product.getOffers().stream().map(this::mapToOfferResponse).collect(Collectors.toList())
                : new ArrayList<>();

        BigDecimal lowestPrice = offerDtos.stream()
                .map(MerchantOfferResponseDto::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .lowestPrice(lowestPrice)
                .offers(offerDtos)
                .build();
    }

    private MerchantOfferResponseDto mapToOfferResponse(MerchantOffer offer) {
        return MerchantOfferResponseDto.builder()
                .id(offer.getId())
                .productId(offer.getProduct().getId())
                .merchant(offer.getMerchant())
                .price(offer.getPrice())
                .originalPrice(offer.getOriginalPrice())
                .productUrl(offer.getProductUrl())
                .inStock(offer.getInStock())
                .rating(offer.getRating())
                .deliveryText(offer.getDeliveryText())
                .lastUpdated(offer.getLastUpdated())
                .build();
    }
}
