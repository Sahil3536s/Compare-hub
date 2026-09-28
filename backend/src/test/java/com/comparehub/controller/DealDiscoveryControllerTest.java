package com.comparehub.controller;

import com.comparehub.dto.SmartDealDto;
import com.comparehub.dto.SmartDealsPageDto;
import com.comparehub.model.SmartDealCategory;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtAuthenticationFilter;
import com.comparehub.security.RateLimitingFilter;
import com.comparehub.service.DealDiscoveryService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealDiscoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DealDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DealDiscoveryService dealDiscoveryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/deals/smart should return paginated smart deals")
    void testGetSmartDeals() throws Exception {
        SmartDealDto deal = SmartDealDto.builder()
                .id("prod-1-1")
                .title("Gaming Laptop")
                .currentPrice(BigDecimal.valueOf(67999))
                .historicalTypicalPrice(BigDecimal.valueOf(82999))
                .realSavingsAmount(BigDecimal.valueOf(15000))
                .realDiscountPercent(18)
                .dealScore(91)
                .dealCategory(SmartDealCategory.EXCEPTIONAL_DEALS)
                .dealLabel("Exceptional Deal")
                .reason("18% below 30-day average")
                .build();

        SmartDealsPageDto pageDto = SmartDealsPageDto.builder()
                .deals(List.of(deal))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .activeCategory(SmartDealCategory.ALL)
                .categoryCounts(Map.of("ALL", 1L, "EXCEPTIONAL_DEALS", 1L))
                .build();

        when(dealDiscoveryService.getPersonalizedSmartDeals(any(), any())).thenReturn(pageDto);

        mockMvc.perform(get("/api/deals/smart?category=ALL&page=0&size=8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.deals[0].title").value("Gaming Laptop"))
                .andExpect(jsonPath("$.deals[0].dealScore").value(91))
                .andExpect(jsonPath("$.deals[0].realDiscountPercent").value(18))
                .andExpect(jsonPath("$.deals[0].dealLabel").value("Exceptional Deal"));
    }
}
