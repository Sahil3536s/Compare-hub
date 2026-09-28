package com.comparehub.controller;

import com.comparehub.dto.ProductResponseDto;
import com.comparehub.dto.UserResponseDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.DatabaseSeedService;
import com.comparehub.service.PriceAlertService;
import com.comparehub.service.ProductService;
import com.comparehub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class DatabaseTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductService productService;

    @MockBean
    private PriceAlertService priceAlertService;

    @MockBean
    private DatabaseSeedService databaseSeedService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnAllUsers() throws Exception {
        UserResponseDto userDto = UserResponseDto.builder()
                .id(1L)
                .name("Alex Hunter")
                .email("alex@example.com")
                .createdAt(Instant.now())
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(userDto));

        mockMvc.perform(get("/api/test/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alex Hunter"))
                .andExpect(jsonPath("$[0].email").value("alex@example.com"));
    }

    @Test
    void shouldReturnAllProducts() throws Exception {
        ProductResponseDto productDto = ProductResponseDto.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .category("Smartphones")
                .lowestPrice(new BigDecimal("127990.00"))
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(productDto));

        mockMvc.perform(get("/api/test/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$[0].lowestPrice").value(127990.00));
    }

    @Test
    void shouldTriggerDatabaseSeed() throws Exception {
        when(databaseSeedService.seedSampleData()).thenReturn("Successfully seeded database");

        mockMvc.perform(post("/api/test/seed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully seeded database"));
    }
}
