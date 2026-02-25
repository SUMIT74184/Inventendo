package org.example.auth.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Dto.AuthDto;
import org.example.auth.Service.AuthService;
import org.example.auth.Util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;


    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse>register(AuthDto.Register request){
        log.info("Registration request for email: {}",request.getEmail());
        AuthDto.AuthResponse response =authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse>login(AuthDto.LoginRequest request){
        log.info("Login request for email: {}",request.getEmail());
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.AuthResponse>refresh(AuthDto.RefreshTokenRequest request){
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

    public ResponseEntity<Map<String,Object>> getCurrentUser(){

    }





}
