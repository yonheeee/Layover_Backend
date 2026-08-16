package com.ssafy.layover.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_PREFIX = "email:code:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final long CODE_TTL = 300L;
    private static final long VERIFIED_TTL = 600L;

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, ExpiringValue> memoryStore = new ConcurrentHashMap<>();

    @Value("${email.verification.storage:memory}")
    private String storageType;

    public void saveCode(String email, String code) {
        if (useMemoryStorage()) {
            memoryStore.put(CODE_PREFIX + email, new ExpiringValue(code, CODE_TTL));
            return;
        }
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL, TimeUnit.SECONDS);
    }

    // 반환: "EXPIRED"(코드 없음/만료), "INVALID"(불일치), "OK"(성공)
    public String verifyCode(String email, String code) {
        String stored = useMemoryStorage()
                ? getMemoryValue(CODE_PREFIX + email)
                : redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (stored == null) {
            return "EXPIRED";
        }
        if (!stored.equals(code)) {
            return "INVALID";
        }
        if (useMemoryStorage()) {
            memoryStore.put(VERIFIED_PREFIX + email, new ExpiringValue("true", VERIFIED_TTL));
            memoryStore.remove(CODE_PREFIX + email);
        } else {
            redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "true", VERIFIED_TTL, TimeUnit.SECONDS);
            redisTemplate.delete(CODE_PREFIX + email);
        }
        return "OK";
    }

    public boolean isVerified(String email) {
        String verified = useMemoryStorage()
                ? getMemoryValue(VERIFIED_PREFIX + email)
                : redisTemplate.opsForValue().get(VERIFIED_PREFIX + email);
        return "true".equals(verified);
    }

    public void deleteVerification(String email) {
        if (useMemoryStorage()) {
            memoryStore.remove(CODE_PREFIX + email);
            memoryStore.remove(VERIFIED_PREFIX + email);
            return;
        }
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    private boolean useMemoryStorage() {
        return !"redis".equalsIgnoreCase(storageType);
    }

    private String getMemoryValue(String key) {
        ExpiringValue value = memoryStore.get(key);
        if (value == null) {
            return null;
        }
        if (value.isExpired()) {
            memoryStore.remove(key);
            return null;
        }
        return value.value();
    }

    private record ExpiringValue(String value, Instant expiresAt) {
        private ExpiringValue(String value, long ttlSeconds) {
            this(value, Instant.now().plusSeconds(ttlSeconds));
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
