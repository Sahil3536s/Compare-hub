package com.comparehub.service;

import com.comparehub.dto.MerchantOfferRequestDto;
import com.comparehub.dto.PriceAlertRequestDto;
import com.comparehub.dto.ProductCreateRequestDto;
import com.comparehub.dto.UserCreateRequestDto;
import com.comparehub.dto.UserResponseDto;
import com.comparehub.dto.ProductResponseDto;
import com.comparehub.repository.ProductRepository;
import com.comparehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSeedService {

    private final UserService userService;
    private final ProductService productService;
    private final PriceAlertService priceAlertService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public String seedSampleData() {
        if (userRepository.count() > 0 && productRepository.count() > 0) {
            return "Database already contains seed data (" + productRepository.count() + " products, " + userRepository.count() + " users).";
        }

        // 1. Create Test User
        UserResponseDto testUser;
        if (!userRepository.existsByEmail("testuser@comparehub.com")) {
            testUser = userService.createUser(UserCreateRequestDto.builder()
                    .name("Alex Hunter")
                    .email("testuser@comparehub.com")
                    .password("Password@123")
                    .build());
        } else {
            testUser = userService.getUserByEmail("testuser@comparehub.com");
        }

        // 2. Create Sample Products with Merchant Offers
        ProductResponseDto iphone = productService.createProduct(ProductCreateRequestDto.builder()
                .name("Apple iPhone 15 Pro (128 GB) - Natural Titanium")
                .brand("Apple")
                .category("Smartphones")
                .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600")
                .offers(List.of(
                        MerchantOfferRequestDto.builder()
                                .merchant("Amazon")
                                .price(new BigDecimal("127990.00"))
                                .originalPrice(new BigDecimal("134900.00"))
                                .productUrl("https://amazon.in/dp/example1")
                                .inStock(true)
                                .rating(new BigDecimal("4.9"))
                                .deliveryText("Free Next-Day Delivery")
                                .build(),
                        MerchantOfferRequestDto.builder()
                                .merchant("Flipkart")
                                .price(new BigDecimal("128990.00"))
                                .originalPrice(new BigDecimal("134900.00"))
                                .productUrl("https://flipkart.com/dp/example1")
                                .inStock(true)
                                .rating(new BigDecimal("4.7"))
                                .deliveryText("Delivery in 2 Days")
                                .build(),
                        MerchantOfferRequestDto.builder()
                                .merchant("Croma")
                                .price(new BigDecimal("129900.00"))
                                .originalPrice(new BigDecimal("134900.00"))
                                .productUrl("https://croma.com/dp/example1")
                                .inStock(true)
                                .rating(new BigDecimal("4.6"))
                                .deliveryText("Store Pickup Today")
                                .build()
                ))
                .build());

        ProductResponseDto headphones = productService.createProduct(ProductCreateRequestDto.builder()
                .name("Sony WH-1000XM5 Wireless Noise Cancelling Headphones")
                .brand("Sony")
                .category("Audio & Headphones")
                .imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600")
                .offers(List.of(
                        MerchantOfferRequestDto.builder()
                                .merchant("Flipkart")
                                .price(new BigDecimal("26990.00"))
                                .originalPrice(new BigDecimal("34990.00"))
                                .productUrl("https://flipkart.com/dp/example2")
                                .inStock(true)
                                .rating(new BigDecimal("4.8"))
                                .deliveryText("Express Delivery Tomorrow")
                                .build(),
                        MerchantOfferRequestDto.builder()
                                .merchant("Amazon")
                                .price(new BigDecimal("27990.00"))
                                .originalPrice(new BigDecimal("34990.00"))
                                .productUrl("https://amazon.in/dp/example2")
                                .inStock(true)
                                .rating(new BigDecimal("4.9"))
                                .deliveryText("Free 2-Day Shipping")
                                .build()
                ))
                .build());

        // 3. Create Sample Price Alert
        priceAlertService.createPriceAlert(PriceAlertRequestDto.builder()
                .userId(testUser.getId())
                .productId(iphone.getId())
                .targetPrice(new BigDecimal("125000.00"))
                .active(true)
                .build());

        return "Successfully seeded database with sample users, products, merchant offers, and price alerts.";
    }
}
