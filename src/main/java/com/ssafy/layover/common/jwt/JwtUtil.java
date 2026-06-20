package com.ssafy.layover.common.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${ssafy.jwt.secret-string}")
    private String secretString;

    @Value("${ssafy.jwt.access-expmin}")
    private long accessExpMin;

    @Value("${ssafy.jwt.refresh-expmin}")
    private long refreshExpMin;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpMin * 60 * 1000))
                .signWith(secretKey())
                .compact();
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey()).build()
                .parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpMin * 60 * 1000))
                .signWith(secretKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
