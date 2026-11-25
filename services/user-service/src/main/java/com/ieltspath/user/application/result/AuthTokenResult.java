package com.ieltspath.user.application.result;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        java.util.UUID refreshTokenId
) {
    public AuthTokenResult(String accessToken, String refreshToken, long accessTokenExpiresInSeconds,
                           long refreshTokenExpiresInSeconds) {
        this(accessToken, refreshToken, accessTokenExpiresInSeconds, refreshTokenExpiresInSeconds, null);
    }
}
