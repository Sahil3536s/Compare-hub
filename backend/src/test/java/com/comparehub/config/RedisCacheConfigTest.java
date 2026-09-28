package com.comparehub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheConfigTest {

    @Test
    void shouldCreateCacheManagerWithGranularCaches() {
        RedisCacheConfig config = new RedisCacheConfig();
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        CacheManager cacheManager = config.cacheManager(provider);

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCT_COMPARISONS));
        assertNotNull(cacheManager.getCache(RedisCacheConfig.CACHE_FLIGHT_SEARCHES));
        assertNotNull(cacheManager.getCache(RedisCacheConfig.CACHE_GEOCODING_LOCATIONS));
        assertNotNull(cacheManager.getCache(RedisCacheConfig.CACHE_PROVIDER_METADATA));
    }

    @Test
    void shouldStoreRetrieveAndEvictCachedData() {
        RedisCacheConfig config = new RedisCacheConfig();
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        CacheManager cacheManager = config.cacheManager(provider);
        Cache cache = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCT_COMPARISONS);
        assertNotNull(cache);

        String testKey = "test_product_query_iphone";
        String testValue = "cached_iphone_results";

        // Store
        cache.put(testKey, testValue);

        // Retrieve
        Cache.ValueWrapper wrapper = cache.get(testKey);
        assertNotNull(wrapper);
        assertEquals(testValue, wrapper.get());

        // Evict
        cache.evict(testKey);
        assertNull(cache.get(testKey));
    }
}
