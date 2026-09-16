package com.group01.user.application.usecase;

import com.group01.user.application.result.AuthTokenResult;
import com.group01.user.config.AuthTokenProperties;
import com.group01.user.domain.aggregate.RefreshToken;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

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
        String accessToken = jwtTokenService.createAccessToken(user);
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHashService.hash(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(properties.refreshTokenMaxAgeSeconds()))
                .build());
        return new AuthTokenResult(
                accessToken,
                refreshToken,
                properties.accessTokenMaxAgeSeconds(),
                properties.refreshTokenMaxAgeSeconds()
        );
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
