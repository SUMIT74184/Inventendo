package org.example.auth.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Dto.AuthDto;
import org.example.auth.Entity.User;
import org.example.auth.Service.CustomOAuth2User;
import org.example.auth.Util.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

/* *
 * OAuth2LoginSuccessHandler: Called after successful OAuth2 login.
 *
 * FLOW:
 * 1. User successfully logs in via Google/Facebook/Apple
 * 2. CustomOAuth2UserService loads/creates User entity
 * 3. Spring Security calls this handler's onAuthenticationSuccess()
 * 4. We generate JWT tokens (access + refresh)
 * 5. Redirect to frontend with tokens in URL query params
 *
 * FRONTEND FLOW:
 * Frontend receives redirect to:
 *   http://localhost:3000/auth/callback?access_token=xxx&refresh_token=yyy&expires_in=900
 *
 * Frontend extracts tokens from URL, stores them (localStorage/cookie), then navigates to dashboard.
 *
 * PRODUCTION: Use POST to /auth/oauth2/callback instead of GET redirect with tokens in URL
 * (tokens in URL are visible in browser history and server logs).
 * But for development, this GET redirect approach is simpler.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String,String>kafkaTemplate;



    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oauth2User.getUser();

        log.info("OAuth2 login success: userId={}, email={}, provider={}",
                user.getId(),user.getEmail(),user.getProvider()
                );

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        //cache refresh token
        redisTemplate.opsForValue().set(
                "refresh_token:"+user.getId(),
                refreshToken,
                Duration.ofDays(7)
        );

        //Publish kafka event for oauth login
        publishOAuth2LoginEvent(user);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
                .queryParam("access_token",accessToken)
                .queryParam("refresh_token",refreshToken)
                .queryParam("expires_in",900)
                .queryParam("token_type","Bearer")
                .build()
                .toUriString();


        /*
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        AuthDto.AuthResponse authResponse = AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .roles(user.getRoles())
                .roles(user.getRoles())
                .build();

        new ObjectMapper().writeValue(response.getWriter(),authResponse);
        return;
        */


        getRedirectStrategy().sendRedirect(request,response,targetUrl);

    }

    private void publishOAuth2LoginEvent(User user) {
        try {
            String topic = "user.login.oauth2";
            String payload = String.format(
                    "{\"userId\":\"%s\",\"email\":\"%s\",\"tenantId\":\"%s\",\"provider\":\"%s\"}",
                    user.getId(), user.getEmail(), user.getTenantId(), user.getProvider()
            );
            kafkaTemplate.send(topic, user.getId(), payload);
            log.debug("Published OAuth2 login event for user: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to publish OAuth2 login event: {}", e.getMessage());
        }
    }
}
