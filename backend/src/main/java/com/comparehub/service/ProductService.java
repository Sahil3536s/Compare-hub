package com.comparehub.service;

import com.comparehub.dto.MerchantOfferRequestDto;
import com.comparehub.dto.MerchantOfferResponseDto;
import com.comparehub.dto.ProductCreateRequestDto;
import com.comparehub.dto.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductCreateRequestDto request);

    ProductResponseDto getProductById(Long id);

    List<ProductResponseDto> getAllProducts();

    Page<ProductResponseDto> getAllProducts(Pageable pageable);

    List<ProductResponseDto> getProductsByCategory(String category);

    MerchantOfferResponseDto addMerchantOffer(Long productId, MerchantOfferRequestDto offerRequest);

    List<MerchantOfferResponseDto> getProductOffers(Long productId);

    void deleteProduct(Long id);
}
