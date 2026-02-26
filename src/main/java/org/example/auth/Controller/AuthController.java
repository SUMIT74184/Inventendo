package org.example.auth.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Dto.AuthDto;
import org.example.auth.Entity.User;
import org.example.auth.Service.AuthService;
import org.example.auth.Util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;


    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse>register(@Valid @RequestBody AuthDto.Register request){
        log.info("Registration request for email: {}",request.getEmail());
        AuthDto.AuthResponse response =authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse>login(@Valid @RequestBody AuthDto.LoginRequest request){
        log.info("Login request for email: {}",request.getEmail());
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.AuthResponse>refresh(@Valid @RequestBody AuthDto.RefreshTokenRequest request){
        log.info("Token refresh request");
        AuthDto.AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    /* *
     * Logout: Blacklist current access token + delete refresh token.
     *
     * How it works:
     * 1. Extract token from Authorization: Bearer <token>
     * 2. Add token to Redis blacklist with TTL = token's remaining lifetime
     * 3. Delete refresh token from Redis
     * 4. Evict user from cache
     * 5. Publish logout event to Kafka
     *
     * After logout, any request with this token will be rejected by JwtAuthenticationFilter.
     */
    @PostMapping("/logout")
    public ResponseEntity<String>logout(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        authService.logout(token,email);
        log.info("User logged out: {}",email);
        return ResponseEntity.ok("Logged out successfully");
    }

    /* *
     * Validate token: Used by API Gateway on every forwarded request.
     *
     * Flow:
     * 1. User makes request to: http://localhost:8080/api/inventory/products
     * 2. API Gateway extracts JWT from Authorization header
     * 3. API Gateway calls: GET http://localhost:8081/api/auth/validate (with same JWT)
     * 4. Auth Service checks: blacklist, expiry, signature
     * 5. Auth Service returns: {valid: true, userId, email, tenantId, roles}
     * 6. API Gateway adds X-User-Id and X-Tenant-Id headers to forwarded request
     * 7. Inventory Service receives request with user context in headers
     *
     * This endpoint is @Cacheable (5 min TTL) to avoid DB load.
     */
    @GetMapping("/validate")
    public ResponseEntity<AuthDto.TokenValidationResponse>validate (@RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        AuthDto.TokenValidationResponse response = authService.validateToken(token);

        if(response.isValid()){
            return ResponseEntity.ok(response);
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Get current user info.
     * Extracts user from SecurityContext (set by JwtAuthenticationFilter).
     * Useful for frontend to display "Welcome, John!" after login.
     */

    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        String userId = jwtUtil.extractUserId(token);
        String tenantId = jwtUtil.extractTenantId(token);

//        if(auth ==  null || !auth.isAuthenticated()){
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }

//        String email = auth.getName();
//        String token = extractTokenFromContext();

//        if (token!=null){
//            String userId = jwtUtil.extractUserId(token);
//            String tenantId = jwtUtil.extractTenantId(token);

            Map<String, Object>userInfo = new HashMap<>();
            userInfo.put("email",email);
            userInfo.put("userId", userId);
            userInfo.put("tenantId", tenantId);
            userInfo.put("authorities",auth!=null ? auth.getAuthorities(): List.of());

            return ResponseEntity.ok(userInfo);
        }

//        return ResponseEntity.ok(userInfo);



    @GetMapping("/oauth2/error")
    public ResponseEntity<Map<String,String>> oauth2Error(
         @RequestParam(required = false) String error,
         @RequestParam(required = false) String error_description

    ){
        log.error("OAuth2 login error: {} - {}",error,error_description);

        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("error",error!=null?error:"oauth2_login_failed");
        errorResponse.put("message",error_description!=null ? error_description : "OAuth2 login failed. Please try again." );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

    }

    /**
     * Admin endpoint: Get all users for a tenant.
     * Only TENANT_ADMIN or SUPER_ADMIN can call this.
     */

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String tenantId = jwtUtil.extractTenantId(token);

        List<User> users = authService.getUsersByTenant(tenantId);

        Map<String, Object> response = new HashMap<>();
        response.put("tenantId", tenantId);
        response.put("totalUsers", users.size());
        response.put("users", users.stream().map(this::toUserSummary).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for Kubernetes liveness/readiness probes.
     * Returns 200 OK if service is healthy.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String,String>> health(){
        Map<String,String>health = new HashMap<>();
        health.put("status","UP");
        health.put("Service","auth-service");
        return ResponseEntity.ok(health);
    }
    /**
     * Convert User entity to a safe summary map (excludes password).
     * Used when returning user lists to admins.
     */
    private Map<String, Object> toUserSummary(User user) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", user.getId());
        summary.put("email", user.getEmail());
        summary.put("firstName", user.getFirstName());
        summary.put("lastName", user.getLastName());
        summary.put("roles", user.getRoles());
        summary.put("provider", user.getProvider());
        summary.put("profilePictureUrl", user.getProfilePictureUrl());
        summary.put("enabled", user.isEnabled());
        summary.put("accountNonLocked", user.isAccountNonLocked());
        summary.put("createdAt", user.getCreatedAt());
        return summary;
    }

    private String extractTokenFromContext(){
        return null;
    }








}
