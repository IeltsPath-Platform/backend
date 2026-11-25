package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.config.AuthTokenProperties;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import com.ieltspath.user.application.command.LoginContext;

@Service
@RequiredArgsConstructor
public class AuthTokenIssuer {
    private static final int REFRESH_TOKEN_BYTES = 48;

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final AuthTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenResult issue(User user) {
        UUID familyId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now();
        return issue(user, familyId, startedAt, LoginContext.UNKNOWN);
    }

    public AuthTokenResult issue(User user, UUID familyId, LocalDateTime sessionStartedAt, LoginContext context) {
        String accessToken = jwtTokenService.createAccessToken(user);
        String refreshToken = newRefreshToken();
        RefreshToken stored = refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHashService.hash(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(properties.refreshTokenMaxAgeSeconds()))
                .familyId(familyId)
                .sessionStartedAt(sessionStartedAt)
                .lastUsedAt(LocalDateTime.now())
                .ipAddress(context.ipAddress())
                .userAgent(context.userAgent())
                .build());
        return new AuthTokenResult(
                accessToken,
                refreshToken,
                properties.accessTokenMaxAgeSeconds(),
                properties.refreshTokenMaxAgeSeconds(),
                stored.getId()
        );
    }

    public long reuseGraceSeconds() {
        return properties.refreshReuseGraceSeconds();
    }

    public boolean sessionExpired(LocalDateTime startedAt, LocalDateTime now) {
        long maximum = properties.sessionMaxAgeSeconds();
        return maximum > 0 && startedAt != null && startedAt.plusSeconds(maximum).isBefore(now);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
