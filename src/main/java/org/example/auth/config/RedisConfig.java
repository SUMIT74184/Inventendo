package org.example.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/* *
        * RedisConfig: Sets up TWO things —
        *
        * 1. RedisTemplate<String,String>: Used for MANUAL Redis operations in code.
 *    Example: redisTemplate.opsForValue().set("blacklisted_token:xyz", "true", 15, MINUTES)
 *    We use this for token blacklist and refresh token storage.
        *
        * 2. CacheManager: Used by Spring's @Cacheable/@CacheEvict ANNOTATIONS.
        *    Example: @Cacheable("users") on a service method → result auto-stored in Redis.
 *    @EnableCaching on the main class activates this system.
 *    Without CacheManager bean, @Cacheable would use in-memory cache (lost on restart).
        */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate: Low-level Redis access with String keys and String values.
     * StringRedisSerializer stores data as plain text — readable in Redis CLI.
     * Used by: AuthService (refresh tokens), JwtAuthenticationFilter (blacklist check).
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager: The Spring Cache abstraction layer backed by Redis.
     * This is what @Cacheable("users") uses under the hood.
     * <p>
     * RedisCacheConfiguration: Controls how cache entries are stored.
     * - entryTtl: How long cached data lives before Redis auto-deletes it.
     * - disableCachingNullValues: Never cache null results (avoids ghost cache entries).
     * - serializeValuesWith JSON: Stores objects as JSON, not Java binary format.
     * JSON is human-readable in Redis CLI: GET "users::user@example.com"
     * <p>
     * perCacheConfig: Different TTL per cache name.
     * "users" cache: 30 min — user data changes rarely
     * "token-validation": 5 min — short TTL because tokens expire and get blacklisted
     * "tenant-users": 15 min — tenant user lists change occasionally
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();

        perCacheConfig.put("users",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        perCacheConfig.put("token-validation",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        perCacheConfig.put("tenant-users",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
