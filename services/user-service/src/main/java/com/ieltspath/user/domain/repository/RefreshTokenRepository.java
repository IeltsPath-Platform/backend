package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> consumeActiveToken(String tokenHash, java.time.LocalDateTime consumedAt);
}
