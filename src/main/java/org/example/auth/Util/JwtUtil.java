package org.example.auth.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;
import org.example.auth.Entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;


    /**
     * Generate an access token for a user.
     * JWT = Header.Payload.Signature
     * Payload (claims) holds: who the user is, their roles, their tenant, etc.
     */

    public String generateAccessToken(User user){
        Map<String,Object> extraClaims= new HashMap<>();
        extraClaims.put("userId",user.getId());
        extraClaims.put("tenantId",user.getTenantId());
        extraClaims.put("roles",user.getRoles());
        extraClaims.put("firstName",user.getFirstName());
        extraClaims.put("tokenType","ACCESS");
        return buildToken(extraClaims,user.getEmail(),accessTokenExpiration);

    }

    /**
     * Refresh token has minimal claims — it's only used to get a new access token.
     * We don't put role/tenant info here because it might change and the old token
     * would have stale data.
     */

    public String generateRefreshToken(User user){
        Map<String,Object> extraClaims = new HashMap<>();
        extraClaims.put("userId",user.getId());
        extraClaims.put("tokenType","REFRESH");
        return buildToken(extraClaims,user.getEmail(),refreshTokenExpiration);
    }

    private String buildToken(Map<String,Object>extraClaims,String subject,long expiration){
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();

    }

    /**
     * Extract the email (subject) from a token.
     * This is used to look up the user in DB when validating requests.
     */
    public String extractEmail(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token){
        return extractClaim(token,claims->claims.get("userId",String.class));
    }

    public String extractTenantId(String token){
        return extractClaim(token,claims->claims.get("TenantId",String.class));
    }

    public Date extractExpiration(String token){
        return extractClaim(token,Claims::getExpiration);
    }

    /**
     * Generic claim extractor using a Function (Java functional programming).
     * claimsResolver is a function like: claims -> claims.getSubject()
     * This pattern avoids writing a new method for every claim you want to extract.
     */

    public <T> T extractClaim(String token, Function<Claims,T>claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token,String email){
        try{
            final String tokenEmail = extractEmail((token));
            return (tokenEmail.equals(email)) && !isTokenExpired(token);

        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private SecretKey getSigningKey(){
        byte[] KeyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(KeyBytes);
    }
}
