package com.comparehub.controller;

import com.comparehub.dto.AuthResponseDto;
import com.comparehub.dto.AuthUserDto;
import com.comparehub.dto.LoginRequestDto;
import com.comparehub.dto.RegisterRequestDto;
import com.comparehub.security.CustomUserDetailsService;
import com.comparehub.security.JwtAuthenticationEntryPoint;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .password("password123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("jwt_sample_token")
                .user(new AuthUserDto(1L, "Alice Smith", "alice@example.com"))
                .build();

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt_sample_token"))
                .andExpect(jsonPath("$.user.name").value("Alice Smith"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void shouldFailValidationOnInvalidEmail() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Smith")
                .email("invalid-email-format")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("password123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("jwt_sample_token")
                .user(new AuthUserDto(1L, "Alice Smith", "alice@example.com"))
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_sample_token"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void shouldReturnUnauthorizedOnBadCredentials() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "Alice Smith", "alice@example.com", "hash", Collections.emptyList());
        AuthUserDto userDto = new AuthUserDto(1L, "Alice Smith", "alice@example.com");

        when(authService.getCurrentUser(any())).thenReturn(userDto);

        mockMvc.perform(get("/api/auth/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }
}
