package com.comparehub.service;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.MerchantOffer;
import com.comparehub.model.PriceAlert;
import com.comparehub.model.Product;
import com.comparehub.model.SavedProduct;
import com.comparehub.model.User;
import com.comparehub.repository.*;
import com.comparehub.service.impl.SavedProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedProductServiceTest {

    @Mock
    private SavedProductRepository savedProductRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MerchantOfferRepository merchantOfferRepository;

    @Mock
    private ProductPriceHistoryRepository productPriceHistoryRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @InjectMocks
    private SavedProductServiceImpl savedProductService;

    private User userA;
    private User userB;
    private Product product1;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(1L).name("User A").email("usera@example.com").build();
        userB = User.builder().id(2L).name("User B").email("userb@example.com").build();
        product1 = Product.builder().id(101L).name("Samsung Galaxy S24").brand("Samsung").category("Smartphones").build();
    }

    @Test
    @DisplayName("Should successfully save product with explicit price and merchant")
    void testSaveProduct_withExplicitPrice() {
        SavedProductRequestDto request = SavedProductRequestDto.builder()
                .userId(1L)
                .productId(101L)
                .savedPrice(new BigDecimal("59999.00"))
                .savedMerchant("Amazon")
                .build();

        when(savedProductRepository.existsByUserIdAndProductId(1L, 101L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        SavedProduct savedEntity = SavedProduct.builder()
                .id(10L)
                .user(userA)
                .product(product1)
                .savedPrice(new BigDecimal("59999.00"))
                .savedMerchant("Amazon")
                .createdAt(Instant.now())
                .build();

        when(savedProductRepository.save(any(SavedProduct.class))).thenReturn(savedEntity);
        when(merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(101L))
                .thenReturn(List.of(MerchantOffer.builder().merchant("Amazon").price(new BigDecimal("59999.00")).build()));

        SavedProductResponseDto response = savedProductService.saveProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSavedPrice()).isEqualByComparingTo(new BigDecimal("59999.00"));
        assertThat(response.getSavedMerchant()).isEqualTo("Amazon");
        verify(savedProductRepository, times(1)).save(any(SavedProduct.class));
    }

    @Test
    @DisplayName("Should reject duplicate save for the same user and product")
    void testSaveProduct_duplicateRejection() {
        SavedProductRequestDto request = SavedProductRequestDto.builder()
                .userId(1L)
                .productId(101L)
                .build();

        when(savedProductRepository.existsByUserIdAndProductId(1L, 101L)).thenReturn(true);

        assertThatThrownBy(() -> savedProductService.saveProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already saved by this user");

        verify(savedProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate exact price drop when current price is lower than saved price")
    void testGetSavedProducts_priceDropCalculation() {
        // Saved at: ₹60,000, Current: ₹56,500 -> Price Drop: ₹3,500 (5.8%)
        SavedProduct saved = SavedProduct.builder()
                .id(10L)
                .user(userA)
                .product(product1)
                .savedPrice(new BigDecimal("60000.00"))
                .savedMerchant("Amazon")
                .createdAt(Instant.now())
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(savedProductRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(saved));

        MerchantOffer liveOffer = MerchantOffer.builder()
                .merchant("Flipkart")
                .price(new BigDecimal("56500.00"))
                .inStock(true)
                .productUrl("https://flipkart.com/samsung-s24")
                .build();

        when(merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(101L))
                .thenReturn(List.of(liveOffer));
        when(priceAlertRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

        List<SavedProductResponseDto> results = savedProductService.getSavedProductsByUser(1L);

        assertThat(results).hasSize(1);
        SavedProductResponseDto dto = results.get(0);

        assertThat(dto.getIsPriceDropped()).isTrue();
        assertThat(dto.getPriceDropAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(dto.getPriceDropPercentage()).isEqualTo(5.8);
        assertThat(dto.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("56500.00"));
        assertThat(dto.getCurrentMerchant()).isEqualTo("Flipkart");
        assertThat(dto.getPriceChange()).isEqualByComparingTo(new BigDecimal("-3500.00"));
    }

    @Test
    @DisplayName("Should not show price drop when current price is equal or higher than saved price")
    void testGetSavedProducts_noPriceDropWhenHigher() {
        SavedProduct saved = SavedProduct.builder()
                .id(10L)
                .user(userA)
                .product(product1)
                .savedPrice(new BigDecimal("50000.00"))
                .savedMerchant("Amazon")
                .createdAt(Instant.now())
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(savedProductRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(saved));

        MerchantOffer liveOffer = MerchantOffer.builder()
                .merchant("Amazon")
                .price(new BigDecimal("52000.00"))
                .inStock(true)
                .build();

        when(merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(101L))
                .thenReturn(List.of(liveOffer));
        when(priceAlertRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

        List<SavedProductResponseDto> results = savedProductService.getSavedProductsByUser(1L);

        assertThat(results).hasSize(1);
        SavedProductResponseDto dto = results.get(0);

        assertThat(dto.getIsPriceDropped()).isFalse();
        assertThat(dto.getPriceDropAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPriceDropPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("User A cannot delete User B's saved product (Strict Tenant Isolation)")
    void testRemoveSavedProductById_userIsolation() {
        // User A tries to delete saved item belonging to User B
        when(savedProductRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedProductService.removeSavedProductById(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found for current user");

        verify(savedProductRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Should include active alert status when alert exists for saved product")
    void testGetSavedProducts_withActiveAlert() {
        SavedProduct saved = SavedProduct.builder()
                .id(10L)
                .user(userA)
                .product(product1)
                .savedPrice(new BigDecimal("60000.00"))
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(savedProductRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(saved));

        PriceAlert alert = PriceAlert.builder()
                .id(50L)
                .user(userA)
                .product(product1)
                .targetPrice(new BigDecimal("55000.00"))
                .active(true)
                .build();

        when(priceAlertRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(alert));
        when(merchantOfferRepository.findByProductIdAndInStockTrueOrderByPriceAsc(101L)).thenReturn(List.of());

        List<SavedProductResponseDto> results = savedProductService.getSavedProductsByUser(1L);

        assertThat(results.get(0).getHasActiveAlert()).isTrue();
        assertThat(results.get(0).getAlertTargetPrice()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(results.get(0).getAlertId()).isEqualTo(50L);
    }
}
