package com.comparehub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;

    @GetMapping("/names")
    public ResponseEntity<Collection<String>> getCacheNames() {
        return ResponseEntity.ok(cacheManager.getCacheNames());
    }

    @PostMapping("/evict/{cacheName}")
    public ResponseEntity<Map<String, Object>> evictCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        Map<String, Object> response = new HashMap<>();

        if (cache != null) {
            cache.clear();
            log.info("Cache '{}' cleared successfully.", cacheName);
            response.put("cache", cacheName);
            response.put("status", "EVICTED");
            response.put("message", "Cache cleared successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("cache", cacheName);
            response.put("status", "NOT_FOUND");
            response.put("message", "Cache does not exist");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/evict-all")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("All caches evicted: {}", cacheNames);
        Map<String, Object> response = new HashMap<>();
        response.put("evictedCaches", cacheNames);
        response.put("status", "ALL_EVICTED");
        return ResponseEntity.ok(response);
    }
}
