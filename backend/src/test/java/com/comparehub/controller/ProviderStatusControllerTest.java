package com.comparehub.controller;

import com.comparehub.dto.ProviderStatusDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.service.ProviderMonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProviderStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderMonitoringService providerMonitoringService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnProvidersStatus() throws Exception {
        ProviderStatusDto status1 = ProviderStatusDto.builder()
                .providerName("Amazon")
                .enabled(true)
                .mode("MOCK_FALLBACK")
                .status("UP")
                .circuitBreakerState("CLOSED")
                .responseTimeMs(45L)
                .lastChecked(Instant.now())
                .build();

        ProviderStatusDto status2 = ProviderStatusDto.builder()
                .providerName("OpenCommerce")
                .enabled(true)
                .mode("LIVE_AUTHORIZED_API")
                .status("UP")
                .circuitBreakerState("CLOSED")
                .responseTimeMs(120L)
                .lastChecked(Instant.now())
                .build();

        when(providerMonitoringService.getProvidersStatus()).thenReturn(List.of(status1, status2));

        mockMvc.perform(get("/api/providers/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerName").value("Amazon"))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[1].providerName").value("OpenCommerce"))
                .andExpect(jsonPath("$[1].mode").value("LIVE_AUTHORIZED_API"));
    }
}
