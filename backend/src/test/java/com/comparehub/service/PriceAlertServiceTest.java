package com.comparehub.service;

import com.comparehub.dto.PriceAlertRequestDto;
import com.comparehub.dto.PriceAlertResponseDto;
import com.comparehub.model.PriceAlert;
import com.comparehub.model.Product;
import com.comparehub.model.User;
import com.comparehub.repository.PriceAlertRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.service.impl.PriceAlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PriceAlertServiceImpl priceAlertService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("john@example.com").name("John Doe").build();
        testProduct = Product.builder().id(10L).name("MacBook Pro").category("Electronics").build();
    }

    @Test
    void shouldCreatePriceAlertSuccessfully() {
        PriceAlertRequestDto request = PriceAlertRequestDto.builder()
                .userId(1L)
                .productId(10L)
                .targetPrice(new BigDecimal("150000.00"))
                .build();

        PriceAlert savedAlert = PriceAlert.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .targetPrice(new BigDecimal("150000.00"))
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenReturn(savedAlert);

        PriceAlertResponseDto response = priceAlertService.createPriceAlert(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("MacBook Pro", response.getProductName());
        assertEquals(new BigDecimal("150000.00"), response.getTargetPrice());
        assertTrue(response.getActive());
    }

    @Test
    void shouldRetrieveUserAlerts() {
        PriceAlert alert = PriceAlert.builder()
                .id(101L)
                .user(testUser)
                .product(testProduct)
                .targetPrice(new BigDecimal("140000.00"))
                .active(true)
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(priceAlertRepository.findByUserId(1L)).thenReturn(List.of(alert));

        List<PriceAlertResponseDto> list = priceAlertService.getAlertsByUserId(1L);

        assertEquals(1, list.size());
        assertEquals(101L, list.get(0).getId());
    }

    @Test
    void shouldDeleteAlert() {
        when(priceAlertRepository.existsById(101L)).thenReturn(true);
        doNothing().when(priceAlertRepository).deleteById(101L);

        assertDoesNotThrow(() -> priceAlertService.deleteAlert(101L));
        verify(priceAlertRepository, times(1)).deleteById(101L);
    }
}
