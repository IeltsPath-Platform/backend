package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<java.util.UUID> findUserIdByTokenHash(String tokenHash);
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);
    Optional<RefreshToken> consumeActiveToken(String tokenHash, java.time.LocalDateTime consumedAt, String reason,
                                               java.util.UUID replacedById, java.time.LocalDateTime lastUsedAt,
                                               String ipAddress, String userAgent);
    int revokeAllActiveForUser(java.util.UUID userId, java.time.LocalDateTime revokedAt, String reason);
    int revokeFamily(java.util.UUID familyId, java.time.LocalDateTime revokedAt, String reason);
    java.util.List<RefreshToken> findActiveSessions(java.util.UUID userId, java.time.LocalDateTime now, int limit);
    int revokeSession(java.util.UUID userId, java.util.UUID familyId, java.time.LocalDateTime revokedAt, String reason);
    int revokeAllActiveExceptSession(java.util.UUID userId, java.util.UUID currentFamilyId, java.time.LocalDateTime revokedAt, String reason);
    int deleteExpiredOrOldRevoked(java.time.LocalDateTime now, java.time.LocalDateTime revokedBefore, int limit);
    int updateReplacement(String tokenHash, java.util.UUID replacedById);
}
