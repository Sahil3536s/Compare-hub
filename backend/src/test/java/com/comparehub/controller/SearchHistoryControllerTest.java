package com.comparehub.controller;

import com.comparehub.dto.SearchHistoryResponseDto;
import com.comparehub.model.SearchType;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.SearchHistoryService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchHistoryService searchHistoryService;

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
    void shouldReturnSearchHistory() throws Exception {
        SearchHistoryResponseDto history = SearchHistoryResponseDto.builder()
                .id(1L)
                .userId(1L)
                .query("iPhone 15")
                .searchType(SearchType.SHOPPING)
                .createdAt(Instant.now())
                .build();

        when(searchHistoryService.getUserSearchHistory(1L)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].query").value("iPhone 15"))
                .andExpect(jsonPath("$[0].searchType").value("SHOPPING"));
    }

    @Test
    void shouldClearHistory() throws Exception {
        mockMvc.perform(delete("/api/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search history cleared successfully"));
    }

    @Test
    void shouldDeleteSingleHistoryItem() throws Exception {
        mockMvc.perform(delete("/api/history/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("History item deleted successfully"));
    }
}
