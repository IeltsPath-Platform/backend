package com.ieltspath.access.infrastructure.crypto;

import com.ieltspath.access.application.port.KeyCodeCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM with a fresh random nonce for every code. The stored text is Base64 of nonce followed by ciphertext and tag,
 * so two keys with the same code (never expected) still look different, and a changed byte fails the tag check.
 * The secret comes from {@code ACCESS_KEY_CODE_SECRET} (Base64, 32 bytes) and has no default.
 */
@Component
public class AesGcmKeyCodeCipher implements KeyCodeCipher {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public AesGcmKeyCodeCipher(@Value("${access.key-code.secret:}") String secret) {
        this.key = parse(secret);
    }

    @Override
    public boolean available() {
        return key != null;
    }

    @Override
    public String encrypt(String code) {
        requireKey();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array());
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Không mã hóa được mã key", failure);
        }
    }

    @Override
    public String decrypt(String stored) {
        requireKey();
        try {
            byte[] raw = Base64.getDecoder().decode(stored);
            if (raw.length <= NONCE_BYTES) throw new IllegalStateException("Mã key đã lưu không hợp lệ");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, raw, 0, NONCE_BYTES));
            return new String(cipher.doFinal(raw, NONCE_BYTES, raw.length - NONCE_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            // No detail: the message must not help anyone work out what was stored.
            throw new IllegalStateException("Không giải mã được mã key đã lưu");
        }
    }

    private void requireKey() {
        if (key == null) throw new IllegalStateException("Chưa cấu hình khóa mã hóa mã key");
    }

    /** A missing or malformed secret leaves the cipher unavailable instead of stopping the whole service. */
    private static SecretKey parse(String secret) {
        if (secret == null || secret.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(secret.trim());
            return bytes.length == 32 ? new SecretKeySpec(bytes, "AES") : null;
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
