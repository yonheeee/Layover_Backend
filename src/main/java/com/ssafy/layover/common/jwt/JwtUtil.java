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

    /** 토큰 종류 클레임. access와 refresh를 구분하기 위해 사용한다. */
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

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
                .claim(CLAIM_TYPE, TYPE_ACCESS)
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
                .claim(CLAIM_TYPE, TYPE_REFRESH)
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

    /**
     * 토큰 종류를 확인한다.
     *
     * <p>예전에는 access와 refresh를 같은 키로 서명하면서 종류를 구분하는 값이 없어,
     * /api/auth/refresh에 access token을 넣어도 통과했다. 그 결과 유출된 access token
     * 하나로 만료 시간마다 무한히 재발급받을 수 있었다. 특히 SSE 연결은 토큰을 URL
     * 쿼리스트링으로 받기 때문에 액세스 로그·브라우저 히스토리에 남을 경로가 이미 있다.
     *
     * <p>typ 클레임이 없는 예전 토큰은 유효하지 않은 것으로 본다.
     * 배포 후 기존 사용자는 한 번 다시 로그인해야 한다.
     */
    public boolean isTokenType(String token, String expectedType) {
        try {
            String type = Jwts.parser().verifyWith(secretKey()).build()
                    .parseSignedClaims(token).getPayload().get(CLAIM_TYPE, String.class);
            return expectedType.equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}
