package org.example.auth.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Dto.AuthDto;
import org.example.auth.Dto.AuthDto.*;
import org.example.auth.Entity.Role;
import org.example.auth.Entity.User;
import org.example.auth.Repository.UserRepository;
import org.example.auth.Util.JwtUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthDto.AuthResponse register(Register request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Set<Role> roles = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles()
                : Set.of(Role.STAFF);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .tenantId(request.getTenantId())
                .roles(roles)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} for tenant: {}", savedUser.getEmail(), savedUser.getTenantId());

        publishEvent("user.registered", buildUserEventPayload(savedUser));

        String accessToken = jwtUtil.generateAccessToken(savedUser);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser);
        cacheRefreshToken(savedUser.getId(), refreshToken);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        cacheRefreshToken(user.getId(), refreshToken);

        log.info("User logged in: {}", user.getEmail());
        publishEvent("user.login", buildUserEventPayload(user));

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String email = jwtUtil.extractEmail(refreshToken);
        String userId = jwtUtil.extractUserId(refreshToken);

        String cachedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);
        if (cachedToken == null || !cachedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        cacheRefreshToken(userId, newRefreshToken);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    /**
     * Logout: blacklist token in Redis AND evict the user from the "users" cache.
     *
     * @CacheEvict(value="users", key="#userEmail"):
     * After logout, remove the cached UserDetails so if the user is deleted/locked
     * between logout and next login, the stale cached data isn't served.
     *
     * Why? Without eviction: A deleted user's data lives in cache for 30 min.
     * Their requests would still pass the cache check even after DB deletion.
     */
    @CacheEvict(value = "users", key = "#userEmail")
    public void logout(String token, String userEmail) {
        String userId = jwtUtil.extractUserId(token);
        long expirationTime = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();

        if (expirationTime > 0) {
            redisTemplate.opsForValue().set(
                    "blacklisted_token:" + token,
                    "true",
                    expirationTime,
                    TimeUnit.MILLISECONDS
            );
        }

        redisTemplate.delete("refresh_token:" + userId);

        publishEvent("user.logout", String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\"}", userId, userEmail));

        log.info("User logged out, cache evicted for: {}", userEmail);
    }

    /* *
     * validateToken: Called by other services to verify a JWT.
     *
     * @Cacheable("token-validation"): Cache the validation result for 5 minutes.
     * Why? API Gateway calls this on EVERY forwarded request.
     * Without cache: 100 requests/sec = 100 DB reads/sec just for token validation.
     * With cache: Only the first request per token hits DB. Rest served from Redis.
     *
     * key = "#token": Each token is cached independently.
     * The TTL (5 min) is set in RedisConfig under "token-validation" cache name.
     */
    @Cacheable(value = "token-validation", key = "#token", unless = "!#result.valid")
    public AuthDto.TokenValidationResponse validateToken(String token) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted_token:" + token))) {
                return AuthDto.TokenValidationResponse.builder()
                        .valid(false)
                        .message("Token has been revoked")
                        .build();
            }

            String email = jwtUtil.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!jwtUtil.isTokenValid(token, email)) {
                return AuthDto.TokenValidationResponse.builder()
                        .valid(false)
                        .message("Token is invalid or expired")
                        .build();
            }

            return AuthDto.TokenValidationResponse.builder()
                    .valid(true)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .tenantId(user.getTenantId())
                    .roles(user.getRoles())
                    .build();

        } catch (Exception e) {
            return AuthDto.TokenValidationResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build();
        }
    }

    private void cacheRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                "refresh_token:" + userId,
                refreshToken,
                Duration.ofDays(7)
        );
    }
    /* *
     * Get all users for a specific tenant.
     * Used by TENANT_ADMIN to manage their team.
     *
     * @Cacheable("tenant-users"): Cached for 15 min to avoid DB load on repeated admin page visits.
     * Cache is evicted when any user in the tenant is updated/deleted.
     */
    @Cacheable(value = "tenant-users", key = "#tenantId")
    public List<User> getUsersByTenant(String tenantId) {
        log.debug("Loading users for tenant: {} from DB (cache miss)", tenantId);
        return userRepository.findByTenantId(tenantId);
    }

    /**
     * Evict the tenant-users cache when a user is added/removed/updated.
     * Call this from user management endpoints (create/update/delete user).
     */
    @CacheEvict(value = "tenant-users", key = "#tenantId")
    public void evictTenantUsersCache(String tenantId) {
        log.debug("Evicting tenant-users cache for: {}", tenantId);
    }
    /**
     * publishEvent: Sends a structured JSON payload to a Kafka topic.
     *
     * Key = userId/email so Kafka routes messages for the same user
     * to the same partition → guarantees ordering per user.
     * (e.g., user.login always arrives before user.logout for the same user)
     */
    private void publishEvent(String topic, String payload) {
        try {
            kafkaTemplate.send(topic, payload);
            log.debug("Published event to topic {}: {}", topic, payload);
        } catch (Exception e) {
            log.warn("Failed to publish event to topic {}: {}", topic, e.getMessage());
        }
    }

    private String buildUserEventPayload(User user) {
        return String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\",\"tenantId\":\"%s\",\"roles\":\"%s\"}",
                user.getId(), user.getEmail(), user.getTenantId(), user.getRoles()
        );
    }

    private AuthDto.AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .roles(user.getRoles())
                .build();
    }
}