package com.ssafy.layover.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_PREFIX = "email:code:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final long CODE_TTL = 300L;
    private static final long VERIFIED_TTL = 600L;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL, TimeUnit.SECONDS);
    }

    // 반환: "EXPIRED"(코드 없음/만료), "INVALID"(불일치), "OK"(성공)
    public String verifyCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (stored == null) {
            return "EXPIRED";
        }
        if (!stored.equals(code)) {
            return "INVALID";
        }
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "true", VERIFIED_TTL, TimeUnit.SECONDS);
        redisTemplate.delete(CODE_PREFIX + email);
        return "OK";
    }

    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(VERIFIED_PREFIX + email));
    }

    public void deleteVerification(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }
}
