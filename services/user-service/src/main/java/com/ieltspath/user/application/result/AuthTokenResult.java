package com.ieltspath.user.application.result;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds
) {
}
