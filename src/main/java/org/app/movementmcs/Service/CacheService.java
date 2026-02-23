package org.app.movementmcs.Service;

import lombok.extern.slf4j.Slf4j;
import org.app.movementmcs.Dto.MovementResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String MOVEMENT_CACHE_PREFIX = "movement:";
    private static final String PRODUCT_MOVEMENTS_PREFIX = "product_movements:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheMovement(String tenantId, Long movementId, MovementResponse response) {
        String key = buildMovementKey(tenantId, movementId);
        redisTemplate.opsForValue().set(key, response, DEFAULT_TTL);
        log.debug("Cached movement: key={}", key);
    }

    public MovementResponse getMovementFromCache(String tenantId, Long movementId) {
        String key = buildMovementKey(tenantId, movementId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for movement: key={}", key);
            return (MovementResponse) cached;
        }
        log.debug("Cache miss for movement: key={}", key);
        return null;
    }

    public void cacheProductMovements(String tenantId, Long productId, List<MovementResponse> movements) {
        String key = buildProductMovementsKey(tenantId, productId);
        redisTemplate.opsForValue().set(key, movements, DEFAULT_TTL);
        log.debug("Cached product movements: key={}, count={}", key, movements.size());
    }

    public List<MovementResponse> getProductMovementsFromCache(String tenantId, Long productId) {
        String key = buildProductMovementsKey(tenantId, productId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for product movements: key={}", key);
            return (List<MovementResponse>) cached;
        }
        log.debug("Cache miss for product movements: key={}", key);
        return null;
    }

    public void invalidateMovementCache(String tenantId, Long movementId) {
        String key = buildMovementKey(tenantId, movementId);
        redisTemplate.delete(key);
        log.debug("Invalidated movement cache: key={}", key);
    }

    public void invalidateProductMovementsCache(String tenantId, Long productId) {
        String key = buildProductMovementsKey(tenantId, productId);
        redisTemplate.delete(key);
        log.debug("Invalidated product movements cache: key={}", key);
    }

    private String buildMovementKey(String tenantId, Long movementId) {
        return MOVEMENT_CACHE_PREFIX + tenantId + ":" + movementId;
    }

    private String buildProductMovementsKey(String tenantId, Long productId) {
        return PRODUCT_MOVEMENTS_PREFIX + tenantId + ":" + productId;
    }
}
