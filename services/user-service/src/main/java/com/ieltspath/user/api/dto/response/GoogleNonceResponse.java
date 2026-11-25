package com.ieltspath.user.api.dto.response;

public record GoogleNonceResponse(String nonce, int expiresInSeconds) {
}
