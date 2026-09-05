package com.comparehub.controller;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.ProductComparisonResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.ProductComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductComparisonService productComparisonService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnComparisonResults() throws Exception {
        NormalizedProductOfferDto offer = NormalizedProductOfferDto.builder()
                .productName("iPhone 15 Pro")
                .merchant("Amazon")
                .price(new BigDecimal("127990.00"))
                .originalPrice(new BigDecimal("134900.00"))
                .currency("INR")
                .rating(4.8)
                .isCheapest(true)
                .discountPercent(5)
                .inStock(true)
                .build();

        ProductComparisonResponseDto response = ProductComparisonResponseDto.builder()
                .query("iphone")
                .totalOffers(1)
                .cheapestPrice(new BigDecimal("127990.00"))
                .cheapestMerchant("Amazon")
                .offers(List.of(offer))
                .build();

        when(productComparisonService.compareProducts(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/products/search")
                        .param("q", "iphone")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("iphone"))
                .andExpect(jsonPath("$.totalOffers").value(1))
                .andExpect(jsonPath("$.cheapestMerchant").value("Amazon"))
                .andExpect(jsonPath("$.offers[0].merchant").value("Amazon"))
                .andExpect(jsonPath("$.offers[0].isCheapest").value(true));
    }
}
