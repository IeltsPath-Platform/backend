package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.ieltspath.user.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

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
    public Optional<java.util.UUID> findUserIdByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findUserIdByTokenHash(tokenHash);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> consumeActiveToken(String tokenHash, LocalDateTime consumedAt, String reason,
                                                      UUID replacedById, LocalDateTime lastUsedAt,
                                                      String ipAddress, String userAgent) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain)
                .filter(token -> token.isUsable(consumedAt))
                .filter(token -> refreshTokenJpaRepository.revokeActiveToken(tokenHash, consumedAt, reason,
                        replacedById, lastUsedAt, ipAddress, userAgent) == 1);
    }

    @Override
    public int revokeAllActiveForUser(UUID userId, LocalDateTime revokedAt, String reason) {
        return refreshTokenJpaRepository.revokeAllActiveForUser(userId, revokedAt, reason);
    }

    @Override
    public int revokeFamily(UUID familyId, LocalDateTime revokedAt, String reason) {
        return refreshTokenJpaRepository.revokeFamily(familyId, revokedAt, reason);
    }

    @Override
    public List<RefreshToken> findActiveSessions(UUID userId, LocalDateTime now, int limit) {
        return refreshTokenJpaRepository.findActiveSessions(userId, now, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public int revokeSession(UUID userId, UUID familyId, LocalDateTime revokedAt, String reason) {
        return refreshTokenJpaRepository.revokeSession(userId, familyId, revokedAt, reason);
    }

    @Override
    public int revokeAllActiveExceptSession(UUID userId, UUID currentFamilyId, LocalDateTime revokedAt, String reason) {
        return refreshTokenJpaRepository.revokeAllActiveExceptSession(userId, currentFamilyId, revokedAt, reason);
    }

    @Override
    public List<UUID> findActiveSessionIds(UUID userId, LocalDateTime now, UUID exceptFamilyId) {
        return refreshTokenJpaRepository.findActiveSessionIds(userId, now, exceptFamilyId);
    }

    @Override
    public int deleteExpiredOrOldRevoked(LocalDateTime now, LocalDateTime revokedBefore, int limit) {
        return refreshTokenJpaRepository.deleteExpiredOrOldRevoked(now, revokedBefore, limit);
    }

    @Override
    public int updateReplacement(String tokenHash, UUID replacedById) {
        return refreshTokenJpaRepository.updateReplacement(tokenHash, replacedById);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .createdAt(entity.getCreatedAt())
                .familyId(entity.getFamilyId())
                .sessionStartedAt(entity.getSessionStartedAt())
                .revokedReason(entity.getRevokedReason())
                .replacedById(entity.getReplacedById())
                .lastUsedAt(entity.getLastUsedAt())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
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
                .familyId(refreshToken.getFamilyId())
                .sessionStartedAt(refreshToken.getSessionStartedAt())
                .revokedReason(refreshToken.getRevokedReason())
                .replacedById(refreshToken.getReplacedById())
                .lastUsedAt(refreshToken.getLastUsedAt())
                .ipAddress(refreshToken.getIpAddress())
                .userAgent(refreshToken.getUserAgent())
                .build();
    }
}
