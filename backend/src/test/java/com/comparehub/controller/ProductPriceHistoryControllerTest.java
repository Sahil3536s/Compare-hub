package com.comparehub.controller;

import com.comparehub.dto.PricePointDto;
import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.PriceHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductPriceHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductPriceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnPriceHistoryResponse() throws Exception {
        ProductPriceHistoryResponseDto response = ProductPriceHistoryResponseDto.builder()
                .productId(1L)
                .productName("iPhone 15")
                .period("30D")
                .currentPrice(new BigDecimal("70000.00"))
                .lowestPrice(new BigDecimal("70000.00"))
                .highestPrice(new BigDecimal("80000.00"))
                .averagePrice(new BigDecimal("75000.00"))
                .currency("INR")
                .analysisText("Current price is 7% below the 30-day average.")
                .pricePoints(List.of(
                        PricePointDto.builder().date("2026-08-15").price(new BigDecimal("80000.00")).merchant("Amazon").build(),
                        PricePointDto.builder().date("2026-09-01").price(new BigDecimal("70000.00")).merchant("Flipkart").build()
                ))
                .build();

        when(priceHistoryService.getPriceHistory(1L, "30D")).thenReturn(response);

        mockMvc.perform(get("/api/products/1/price-history")
                        .param("period", "30D")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.period").value("30D"))
                .andExpect(jsonPath("$.currentPrice").value(70000.00))
                .andExpect(jsonPath("$.analysisText").value("Current price is 7% below the 30-day average."));
    }
}
