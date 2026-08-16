package com.ssafy.layover.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoService(@Value("${chat.crypto.key}") String configuredKey) {
        this.keySpec = new SecretKeySpec(normalizeKey(configuredKey), "AES");
    }

    public EncryptedValue encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new IllegalStateException("데이터 암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String cipherText, String iv) {
        if (cipherText == null || iv == null) return "";
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(TAG_LENGTH_BITS, Base64.getDecoder().decode(iv))
            );
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[복호화 실패]";
        }
    }

    private byte[] normalizeKey(String configuredKey) {
        String key = configuredKey == null ? "" : configuredKey.trim();
        if (!key.isBlank()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(key);
                if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // Base64가 아니면 아래에서 해시 기반 키로 변환합니다.
            }
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("암호화 키 초기화에 실패했습니다.", e);
        }
    }
}
