package com.comparehub.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test-endpoint");
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product with ID 999 not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Product with ID 999 not found", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid email or password", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("You do not have permission to access this resource", response.getBody().getMessage());
    }

    @Test
    void shouldHandleProviderUnavailableException() {
        ProviderUnavailableException ex = new ProviderUnavailableException("Amazon API down");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProviderUnavailable(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("Service Unavailable", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("temporarily unavailable"));
    }

    @Test
    void shouldHandleProviderTimeoutException() {
        TimeoutException ex = new TimeoutException("Gateway timeout");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProviderTimeout(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(504, response.getBody().getStatus());
        assertEquals("Gateway Timeout", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("took too long to respond"));
    }

    @Test
    void shouldHandleRateLimitException() {
        RateLimitException ex = new RateLimitException("Too many requests");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRateLimit(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().getStatus());
        assertEquals("Too Many Requests", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void shouldHandleDatabaseException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseErrors(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Database Error", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("database operation failed"));
    }

    @Test
    void shouldHandleGenericExceptionWithoutExposingStackTraces() {
        NullPointerException ex = new NullPointerException("Internal null reference at com.comparehub.service.SomeClass.doWork");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        // Verify stack trace or internal null pointer message is NOT exposed to client
        assertEquals("An unexpected server error occurred. Please try again later.", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("NullPointerException"));
        assertFalse(response.getBody().getMessage().contains("com.comparehub"));
    }
}
