package com.comparehub.aspect;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheTelemetryAspect {

    private final CacheManager cacheManager;

    @Around("@annotation(cacheable)")
    public Object trackCacheHitMiss(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "default";
        long startTime = System.currentTimeMillis();

        // Execution of the method only occurs on CACHE MISS!
        // When Spring Cache finds a hit, the advice around the actual method is bypassed or can be measured.
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.debug("[CACHE MISS] Target: {}.{}() -> Computed in {}ms. Cache: '{}'",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                duration,
                cacheName);

        setResponseHeader("X-Cache", "MISS");
        return result;
    }

    private void setResponseHeader(String name, String value) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletResponse response = attributes.getResponse();
                if (response != null && !response.isCommitted()) {
                    response.setHeader(name, value);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
