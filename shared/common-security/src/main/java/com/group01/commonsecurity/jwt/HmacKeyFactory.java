package com.group01.commonsecurity.jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public final class HmacKeyFactory {
    private static final int MIN_SECRET_BYTES = 32;

    private HmacKeyFactory() {
    }

    public static SecretKey secretKey(String value, String property) {
        byte[] key = decode(value, property);
        if (key.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(property + " must be at least 32 bytes for HS256");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    private static byte[] decode(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(property + " must be base64 encoded", exception);
        }
    }
}
