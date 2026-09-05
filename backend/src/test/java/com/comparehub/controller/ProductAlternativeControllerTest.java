package com.comparehub.controller;

import com.comparehub.dto.AlternativeCategoryType;
import com.comparehub.dto.ProductAlternativeDto;
import com.comparehub.dto.ProductAlternativesResponseDto;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.ProductAlternativeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductAlternativeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductAlternativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductAlternativeService productAlternativeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/products/alternatives should return categorized alternatives")
    void testGetAlternatives() throws Exception {
        ProductAlternativesResponseDto mockResponse = ProductAlternativesResponseDto.builder()
                .baseProductName("iPhone 17")
                .basePrice(new BigDecimal("70000"))
                .baseCategory("Smartphones")
                .totalAlternatives(1)
                .alternatives(List.of(
                        ProductAlternativeDto.builder()
                                .id("alt-1")
                                .productName("OnePlus 13")
                                .price(new BigDecimal("62000"))
                                .priceDifference(new BigDecimal("-8000"))
                                .categoryType(AlternativeCategoryType.CHEAPER_ALTERNATIVE)
                                .categoryLabel("Cheaper Option")
                                .highlights(List.of("?8,000 cheaper", "+4GB RAM"))
                                .build()
                ))
                .build();

        when(productAlternativeService.getAlternatives(anyString(), any(), anyString(), any(), anyInt()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/products/alternatives")
                        .param("productName", "iPhone 17")
                        .param("price", "70000")
                        .param("category", "Smartphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseProductName").value("iPhone 17"))
                .andExpect(jsonPath("$.alternatives[0].productName").value("OnePlus 13"))
                .andExpect(jsonPath("$.alternatives[0].categoryLabel").value("Cheaper Option"));
    }
}
