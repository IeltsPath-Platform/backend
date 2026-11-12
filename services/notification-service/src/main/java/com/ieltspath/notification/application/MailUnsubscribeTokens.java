package com.ieltspath.notification.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Signed one-click unsubscribe links. The token names the user and the category and expires; it is signed with a secret
 * that lives only in configuration, so a link cannot be forged for another account.
 */
@Component
public class MailUnsubscribeTokens {
    private static final Duration LIFETIME = Duration.ofDays(60);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;

    public MailUnsubscribeTokens(@Value("${notification.mail.unsubscribe-secret:}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(UUID userId, String category, Instant now) {
        if (secret.length == 0) throw new IllegalStateException("Unsubscribe secret is not configured");
        String body = ENCODER.encodeToString((userId + "|" + category + "|" + now.plus(LIFETIME).getEpochSecond())
                .getBytes(StandardCharsets.UTF_8));
        return body + "." + ENCODER.encodeToString(sign(body));
    }

    /** The user and category the token grants, or empty when the token is malformed, forged or expired. */
    public Optional<Claim> verify(String token, Instant now) {
        if (secret.length == 0 || token == null) return Optional.empty();
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return Optional.empty();
        String body = token.substring(0, dot);
        try {
            if (!MessageDigest.isEqual(sign(body), DECODER.decode(token.substring(dot + 1)))) return Optional.empty();
            String[] parts = new String(DECODER.decode(body), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 3 || now.getEpochSecond() > Long.parseLong(parts[2])) return Optional.empty();
            return Optional.of(new Claim(UUID.fromString(parts[0]), parts[1]));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", failure);
        }
    }

    public record Claim(UUID userId, String category) { }
}
