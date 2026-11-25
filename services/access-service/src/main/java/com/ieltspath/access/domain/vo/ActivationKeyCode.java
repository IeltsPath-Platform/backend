package com.ieltspath.access.domain.vo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public record ActivationKeyCode(String rawKey, String codeHash, String codeHint) {

    /** No 0, 1, I or O, so a code read aloud or copied by hand is not misread. */
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /** A new random code in the form IP-XXXX-XXXX-XXXX. */
    public static String generateRaw() {
        StringBuilder code = new StringBuilder("IP");
        for (int group = 0; group < 3; group++) {
            code.append('-');
            for (int i = 0; i < 4; i++) code.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return code.toString();
    }

    public ActivationKeyCode {
        Objects.requireNonNull(codeHash, "codeHash must not be null");
    }

    public static ActivationKeyCode fromRawKey(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Activation key must not be blank");
        }
        String cleanKey = rawKey.trim().toUpperCase();
        String hash = computeSha256(cleanKey);
        String hint = cleanKey.length() >= 4 ? cleanKey.substring(cleanKey.length() - 4) : cleanKey;
        return new ActivationKeyCode(cleanKey, hash, hint);
    }

    public static ActivationKeyCode fromHashAndHint(String codeHash, String codeHint) {
        return new ActivationKeyCode(null, codeHash, codeHint);
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
