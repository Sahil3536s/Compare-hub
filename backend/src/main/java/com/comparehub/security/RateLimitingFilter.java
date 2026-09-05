package com.comparehub.security;

import com.comparehub.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map IP+Bucket -> RequestCount
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();

    private static final int SEARCH_LIMIT_PER_MINUTE = 30;
    private static final int AUTH_LIMIT_PER_MINUTE = 10;
    private static final int DEFAULT_LIMIT_PER_MINUTE = 120;

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        int maxAllowed = getMaxAllowedRequests(path);

        if (maxAllowed > 0 && isRateLimited(clientIp, path, maxAllowed)) {
            log.warn("Rate limit exceeded for client IP {} on path {}", clientIp, path);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(Instant.now().toString())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .message("Rate limit exceeded for endpoint. Please wait a few seconds before retrying.")
                    .path(path)
                    .build();

            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int getMaxAllowedRequests(String path) {
        if (path.contains("/api/auth/login")) {
            return AUTH_LIMIT_PER_MINUTE;
        }
        if (path.contains("/api/products/search") ||
            path.contains("/api/flights/search") ||
            path.contains("/api/rides/compare") ||
            path.contains("/api/search")) {
            return SEARCH_LIMIT_PER_MINUTE;
        }
        return 0; // Uncapped for general lightweight GETs/health
    }

    private boolean isRateLimited(String clientIp, String path, int limit) {
        long currentMinute = System.currentTimeMillis() / 60000;
        String key = clientIp + ":" + getEndpointBucket(path) + ":" + currentMinute;

        RequestCounter counter = requestCounters.compute(key, (k, existing) -> {
            if (existing == null) {
                return new RequestCounter(currentMinute);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        // Periodically cleanup older minute buckets (older than 2 minutes)
        if (requestCounters.size() > 5000) {
            requestCounters.entrySet().removeIf(entry -> entry.getValue().windowStart < (currentMinute - 2));
        }

        return counter.count.get() > limit;
    }

    private String getEndpointBucket(String path) {
        if (path.contains("/api/auth/login")) return "auth_login";
        if (path.contains("/api/products/search")) return "search_products";
        if (path.contains("/api/flights/search")) return "search_flights";
        if (path.contains("/api/rides/compare")) return "search_rides";
        if (path.contains("/api/search")) return "universal_search";
        return "default";
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
