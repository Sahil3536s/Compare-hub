package com.comparehub.controller;

import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CacheController.class)
@AutoConfigureMockMvc(addFilters = false)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnAvailableCacheNames() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of("product-comparisons", "flight-searches"));

        mockMvc.perform(get("/api/cache/names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("product-comparisons"));
    }

    @Test
    void shouldEvictSpecificCacheSuccessfully() throws Exception {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("product-comparisons")).thenReturn(mockCache);

        mockMvc.perform(post("/api/cache/evict/product-comparisons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVICTED"))
                .andExpect(jsonPath("$.cache").value("product-comparisons"));
    }

    @Test
    void shouldEvictAllCachesSuccessfully() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of("product-comparisons", "flight-searches"));

        mockMvc.perform(post("/api/cache/evict-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALL_EVICTED"));
    }
}
