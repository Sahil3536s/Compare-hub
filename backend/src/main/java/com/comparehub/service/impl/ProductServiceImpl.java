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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantOfferRepository merchantOfferRepository;
    private final com.comparehub.service.PriceHistoryService priceHistoryService;

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
