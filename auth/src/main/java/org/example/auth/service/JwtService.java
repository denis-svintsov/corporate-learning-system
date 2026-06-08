package org.example.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {
    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong}")
    private String secret;

    private final SecurityPolicyService securityPolicyService;

    public JwtService(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Set<String> roles) {
        // Используем session_timeout_minutes из security_policy
        int timeoutMinutes = securityPolicyService.getSessionTimeoutMinutes();
        long expirationMillis = timeoutMinutes * 60L * 1000L; // Конвертируем минуты в миллисекунды
        
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Set<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        }
        if (roles instanceof String value && !value.isBlank()) {
            return Set.of(value.toUpperCase());
        }
        return Set.of("USER");
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
