package com.comparehub.controller;

import com.comparehub.dto.PricePredictionResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.PricePredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricePredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PricePredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PricePredictionService pricePredictionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturn200WithSuccessPrediction() throws Exception {
        PricePredictionResponseDto dto = PricePredictionResponseDto.builder()
                .status("SUCCESS")
                .productId(1L)
                .productName("iPhone 15 Pro")
                .currentPrice(new BigDecimal("58999.00"))
                .predictedPrice7d(new BigDecimal("56500.00"))
                .predictedChange(new BigDecimal("-2499.00"))
                .predictedChangePercent(-4.24)
                .recommendation("WAIT")
                .confidenceLabel("Medium")
                .dealQuality("GOOD_DEAL")
                .modelName("RandomForestRegressor")
                .dataPointsUsed(30)
                .build();

        when(pricePredictionService.getPricePrediction(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/1/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.recommendation").value("WAIT"))
                .andExpect(jsonPath("$.dealQuality").value("GOOD_DEAL"))
                .andExpect(jsonPath("$.modelName").value("RandomForestRegressor"));
    }

    @Test
    void shouldReturn200WithInsufficientDataStatus() throws Exception {
        PricePredictionResponseDto dto = PricePredictionResponseDto.builder()
                .status("INSUFFICIENT_DATA")
                .productId(2L)
                .productName("Test Product")
                .dataPointsUsed(5)
                .message("More price history is required.")
                .build();

        when(pricePredictionService.getPricePrediction(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/2/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.predictedPrice7d").doesNotExist());
    }

    @Test
    void shouldReturn200WithMLUnavailableStatus() throws Exception {
        PricePredictionResponseDto dto = PricePredictionResponseDto.builder()
                .status("ML_UNAVAILABLE")
                .productId(3L)
                .message("Price prediction service is temporarily unavailable.")
                .build();

        when(pricePredictionService.getPricePrediction(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/3/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ML_UNAVAILABLE"));
    }

    @Test
    void shouldReturn200WithTemporarilyUnavailableStatus() throws Exception {
        PricePredictionResponseDto dto = PricePredictionResponseDto.builder()
                .status("TEMPORARILY_UNAVAILABLE")
                .productId(3L)
                .message("Price prediction is temporarily unavailable.")
                .build();

        when(pricePredictionService.getPricePrediction(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/3/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEMPORARILY_UNAVAILABLE"));
    }

    @Test
    void shouldReturn200WithInvalidResponseStatus() throws Exception {
        PricePredictionResponseDto dto = PricePredictionResponseDto.builder()
                .status("INVALID_RESPONSE")
                .productId(4L)
                .message("Prediction returned invalid or malformed data.")
                .build();

        when(pricePredictionService.getPricePrediction(4L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/4/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_RESPONSE"));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(pricePredictionService.getPricePrediction(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/products/999/prediction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
