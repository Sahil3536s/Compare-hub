package com.comparehub.service;

import com.comparehub.dto.AuthResponseDto;
import com.comparehub.dto.AuthUserDto;
import com.comparehub.dto.LoginRequestDto;
import com.comparehub.dto.RegisterRequestDto;
import com.comparehub.exception.DuplicateResourceException;
import com.comparehub.model.User;
import com.comparehub.repository.UserRepository;
import com.comparehub.security.JwtTokenProvider;
import com.comparehub.security.UserPrincipal;
import com.comparehub.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alice Smith")
                .email("alice@example.com")
                .passwordHash("hashed_secret123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateTokenFromUserIdAndEmail(eq(1L), eq("alice@example.com"), eq("Alice Smith")))
                .thenReturn("mock_jwt_token_123");

        AuthResponseDto response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token_123", response.getToken());
        assertEquals("Alice Smith", response.getUser().getName());
        assertEquals("alice@example.com", response.getUser().getEmail());
        verify(passwordEncoder, times(1)).encode("secret123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateEmail() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("secret123")
                .build();

        UserPrincipal principal = new UserPrincipal(1L, "Alice Smith", "alice@example.com", "hashed_secret123", Collections.emptyList());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mock_login_token");

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_login_token", response.getToken());
        assertEquals(1L, response.getUser().getId());
        assertEquals("alice@example.com", response.getUser().getEmail());
    }

    @Test
    void shouldThrowExceptionOnBadCredentials() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldReturnCurrentUser() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice Smith", "alice@example.com", "hashed_secret123", Collections.emptyList());

        AuthUserDto result = authService.getCurrentUser(principal);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Alice Smith", result.getName());
        assertEquals("alice@example.com", result.getEmail());
    }
}
