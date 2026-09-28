package com.comparehub.controller;

import com.comparehub.dto.SavedProductRequestDto;
import com.comparehub.dto.SavedProductResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.SavedProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SavedProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavedProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavedProductService savedProductService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldSaveProductSuccessfully() throws Exception {
        SavedProductRequestDto request = SavedProductRequestDto.builder()
                .productId(101L)
                .build();

        SavedProductResponseDto response = SavedProductResponseDto.builder()
                .id(1L)
                .userId(1L)
                .productId(101L)
                .productName("iPhone 15")
                .productCategory("Smartphones")
                .createdAt(Instant.now())
                .build();

        when(savedProductService.saveProduct(any(SavedProductRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/saved/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("iPhone 15"));
    }

    @Test
    void shouldGetSavedProducts() throws Exception {
        SavedProductResponseDto response = SavedProductResponseDto.builder()
                .id(1L)
                .userId(1L)
                .productId(101L)
                .productName("iPhone 15")
                .build();

        when(savedProductService.getSavedProductsByUser(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/saved/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("iPhone 15"));
    }

    @Test
    void shouldDeleteSavedProduct() throws Exception {
        mockMvc.perform(delete("/api/saved/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product removed from saved items successfully"));
    }
}
