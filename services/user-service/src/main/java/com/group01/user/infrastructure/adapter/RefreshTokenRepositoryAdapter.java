package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.RefreshToken;
import com.group01.user.domain.repository.RefreshTokenRepository;
import com.group01.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.group01.user.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return toDomain(refreshTokenJpaRepository.save(toEntity(refreshToken)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> consumeActiveToken(String tokenHash, LocalDateTime consumedAt) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain)
                .filter(token -> token.isUsable(consumedAt))
                .filter(token -> refreshTokenJpaRepository.revokeActiveToken(tokenHash, consumedAt) == 1);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken refreshToken) {
        return RefreshTokenJpaEntity.builder()
                .id(refreshToken.getId())
                .userId(refreshToken.getUserId())
                .tokenHash(refreshToken.getTokenHash())
                .expiresAt(refreshToken.getExpiresAt())
                .revokedAt(refreshToken.getRevokedAt())
                .createdAt(refreshToken.getCreatedAt())
                .build();
    }
}
