package com.group01.user.api.dto.response;

public record AuthTokenResponse(
        String message,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {
}
