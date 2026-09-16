package com.group01.user.application.usecase;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class TokenHashService {
    public String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", exception);
        }
    }
}
