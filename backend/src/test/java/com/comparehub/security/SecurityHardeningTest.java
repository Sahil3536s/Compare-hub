package com.comparehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityHardeningTest {

    @Test
    void shouldEnforceBCryptPasswordHashingWithStrengthFactor() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String rawPassword = "SecurePassword@2026!";
        String encoded = encoder.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$12$") || encoded.startsWith("$2b$12$"));
        assertTrue(encoder.matches(rawPassword, encoded));
        assertFalse(encoder.matches("WrongPassword", encoded));
    }

    @Test
    void shouldTriggerRateLimitOnExcessiveLoginRequests() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.100");

        MockHttpServletResponse response = null;

        // Perform 10 permitted requests
        for (int i = 0; i < 10; i++) {
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        // 11th request must be rejected with HTTP 429 Too Many Requests
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Too Many Requests"));
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void shouldTriggerRateLimitOnExcessiveSearchRequests() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/products/search");
        request.setRemoteAddr("10.0.0.5");

        MockHttpServletResponse response = null;

        // Perform 30 permitted search requests
        for (int i = 0; i < 30; i++) {
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        // 31st request exceeds rate limit
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
    }
}
