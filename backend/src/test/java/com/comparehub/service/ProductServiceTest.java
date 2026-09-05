package com.comparehub.service;

import com.comparehub.dto.MerchantOfferRequestDto;
import com.comparehub.dto.ProductCreateRequestDto;
import com.comparehub.dto.ProductResponseDto;
import com.comparehub.model.MerchantOffer;
import com.comparehub.model.Product;
import com.comparehub.repository.MerchantOfferRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MerchantOfferRepository merchantOfferRepository;

    @Mock
    private PriceHistoryService priceHistoryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void shouldCreateProductWithOffersAndCalculateLowestPrice() {
        Product sampleProduct = Product.builder()
                .id(10L)
                .name("MacBook Pro")
                .brand("Apple")
                .category("Laptops & Computers")
                .createdAt(Instant.now())
                .offers(new ArrayList<>())
                .build();

        MerchantOffer offer1 = MerchantOffer.builder()
                .id(101L)
                .product(sampleProduct)
                .merchant("Amazon")
                .price(new BigDecimal("149900.00"))
                .productUrl("https://amazon.in/mbp")
                .inStock(true)
                .lastUpdated(Instant.now())
                .build();

        MerchantOffer offer2 = MerchantOffer.builder()
                .id(102L)
                .product(sampleProduct)
                .merchant("Flipkart")
                .price(new BigDecimal("145900.00"))
                .productUrl("https://flipkart.com/mbp")
                .inStock(true)
                .lastUpdated(Instant.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(merchantOfferRepository.save(any(MerchantOffer.class))).thenReturn(offer1, offer2);

        ProductCreateRequestDto request = ProductCreateRequestDto.builder()
                .name("MacBook Pro")
                .brand("Apple")
                .category("Laptops & Computers")
                .offers(List.of(
                        MerchantOfferRequestDto.builder()
                                .merchant("Amazon")
                                .price(new BigDecimal("149900.00"))
                                .productUrl("https://amazon.in/mbp")
                                .build(),
                        MerchantOfferRequestDto.builder()
                                .merchant("Flipkart")
                                .price(new BigDecimal("145900.00"))
                                .productUrl("https://flipkart.com/mbp")
                                .build()
                ))
                .build();

        ProductResponseDto response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("MacBook Pro", response.getName());
        assertEquals(2, response.getOffers().size());
        assertEquals(new BigDecimal("145900.00"), response.getLowestPrice());
    }
}
