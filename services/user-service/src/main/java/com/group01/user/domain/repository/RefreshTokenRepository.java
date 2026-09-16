package com.group01.user.domain.repository;

import com.group01.user.domain.aggregate.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> consumeActiveToken(String tokenHash, java.time.LocalDateTime consumedAt);
}
