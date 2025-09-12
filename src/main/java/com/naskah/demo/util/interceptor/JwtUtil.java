package com.naskah.demo.util.interceptor;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JwtUtil {

    public static final String DEFAULT_TOKEN_PREFIX = "Bearer ";

    // Token blacklist (in production, use Redis or database)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.verification.expiration:3600}") // 1 hour for email verification
    private Long verificationTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String username, String name, List<String> roles) {
        return generateToken(username, name, roles, jwtExpiration);
    }

    public String generateToken(String username, String name, List<String> roles, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("name", name)
                .claim("role", String.join(",", roles))
                .claim("tokenType", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateVerificationToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + verificationTokenExpiration * 1000);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("tokenType", "EMAIL_VERIFICATION")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpiration * 7 * 1000)); // 7 times longer than access token

        return Jwts.builder()
                .setSubject(username)
                .claim("tokenType", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (blacklistedTokens.contains(token)) {
                log.warn("Token is blacklisted");
                return false;
            }

            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact is invalid: {}", e.getMessage());
        }
        return false;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
        log.info("Token blacklisted successfully");
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    // Keep existing methods for backward compatibility
    public static String extractAuthToken(String authHeader, String prefix){
        String token = Optional.ofNullable(authHeader).orElse("");
        if (prefix != null && token.startsWith(prefix)){
            return token.replaceFirst(prefix, "");
        }
        return token;
    }

    public Map<String, String> getValueFromToken(String token, String[] attrName) {
        if (StringUtils.isEmpty(token)) return new HashMap<>();

        token = extractAuthToken(token, DEFAULT_TOKEN_PREFIX);
        if (StringUtils.isEmpty(token)) return new HashMap<>();

        try {
            Claims claims = extractAllClaims(token);
            Map<String, String> result = new HashMap<>();

            for(String attr : attrName) {
                Object value = claims.get(attr);
                result.put(attr, value != null ? value.toString() : "");
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to extract value from token!", e);
            return new HashMap<>();
        }
    }
}