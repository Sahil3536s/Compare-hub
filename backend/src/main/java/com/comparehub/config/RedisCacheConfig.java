package com.comparehub.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${app.cache.redis.enabled:true}")
    private boolean redisEnabled;

    // Granular Cache TTLs
    public static final String CACHE_PRODUCT_COMPARISONS = "product-comparisons";
    public static final String CACHE_FLIGHT_SEARCHES = "flight-searches";
    public static final String CACHE_GEOCODING_LOCATIONS = "geocoding-locations";
    public static final String CACHE_PROVIDER_METADATA = "provider-metadata";
    public static final String CACHE_PROVIDER_HEALTH = "provider-health";

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();

        if (redisEnabled && connectionFactory != null) {
            try {
                // Test ping to verify Redis server is alive
                connectionFactory.getConnection().ping();
                log.info("Redis connection established at {}:{}. Initializing RedisCacheManager with granular TTLs.",
                        redisHost, redisPort);
                return createRedisCacheManager(connectionFactory);
            } catch (Exception e) {
                log.warn("Redis ping failed ({}:{}). Falling back seamlessly to high-performance Caffeine cache: {}",
                        redisHost, redisPort, e.getMessage());
            }
        } else {
            log.info("Redis disabled or connection factory unavailable. Using local Caffeine cache manager.");
        }

        return createCaffeineCacheManager();
    }

    private RedisCacheManager createRedisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Granular TTL configuration per resource domain
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Products: 15 minutes TTL
        cacheConfigurations.put(CACHE_PRODUCT_COMPARISONS,
                defaultConfiguration.entryTtl(Duration.ofMinutes(15)));

        // Flights: Short 5 minutes TTL (volatile flight fares)
        cacheConfigurations.put(CACHE_FLIGHT_SEARCHES,
                defaultConfiguration.entryTtl(Duration.ofMinutes(5)));

        // Geocoding & Addresses: 24 hours TTL (geocoordinates are static)
        cacheConfigurations.put(CACHE_GEOCODING_LOCATIONS,
                defaultConfiguration.entryTtl(Duration.ofHours(24)));

        // Provider Metadata: 1 hour TTL
        cacheConfigurations.put(CACHE_PROVIDER_METADATA,
                defaultConfiguration.entryTtl(Duration.ofHours(1)));

        // Provider Health: 5 minutes TTL
        cacheConfigurations.put(CACHE_PROVIDER_HEALTH,
                defaultConfiguration.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private CacheManager createCaffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
                CACHE_PRODUCT_COMPARISONS,
                CACHE_FLIGHT_SEARCHES,
                CACHE_GEOCODING_LOCATIONS,
                CACHE_PROVIDER_METADATA,
                CACHE_PROVIDER_HEALTH
        );
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats());
        return caffeineCacheManager;
    }
}
