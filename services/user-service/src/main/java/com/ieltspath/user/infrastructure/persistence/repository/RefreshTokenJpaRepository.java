package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Query("select token.userId from RefreshTokenJpaEntity token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenJpaEntity token where token.tokenHash = :tokenHash")
    Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenJpaEntity token
               set token.revokedAt = :consumedAt,
                   token.revokedReason = :reason,
                   token.replacedById = :replacedById,
                   token.lastUsedAt = :lastUsedAt,
                   token.ipAddress = :ipAddress,
                   token.userAgent = :userAgent
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
               and token.expiresAt > :consumedAt
            """)
    int revokeActiveToken(
            @Param("tokenHash") String tokenHash,
            @Param("consumedAt") LocalDateTime consumedAt,
            @Param("reason") String reason,
            @Param("replacedById") UUID replacedById,
            @Param("lastUsedAt") LocalDateTime lastUsedAt,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent
    );

    @Modifying
    @Query("update RefreshTokenJpaEntity token set token.revokedAt = :revokedAt, token.revokedReason = :reason where token.userId = :userId and token.revokedAt is null and token.expiresAt > :revokedAt")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("update RefreshTokenJpaEntity token set token.revokedAt = :revokedAt, token.revokedReason = :reason where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Query("select token from RefreshTokenJpaEntity token where token.userId = :userId and token.revokedAt is null and token.expiresAt > :now order by token.sessionStartedAt desc, token.id desc")
    java.util.List<RefreshTokenJpaEntity> findActiveSessions(@Param("userId") UUID userId, @Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("update RefreshTokenJpaEntity token set token.revokedAt = :revokedAt, token.revokedReason = :reason where token.userId = :userId and token.familyId = :familyId and token.revokedAt is null")
    int revokeSession(@Param("userId") UUID userId, @Param("familyId") UUID familyId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("update RefreshTokenJpaEntity token set token.revokedAt = :revokedAt, token.revokedReason = :reason where token.userId = :userId and token.familyId <> :familyId and token.revokedAt is null and token.expiresAt > :revokedAt")
    int revokeAllActiveExceptSession(@Param("userId") UUID userId, @Param("familyId") UUID familyId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("update RefreshTokenJpaEntity token set token.replacedById = :replacedById where token.tokenHash = :tokenHash")
    int updateReplacement(@Param("tokenHash") String tokenHash, @Param("replacedById") UUID replacedById);

    @Modifying
    @Query(value = "delete from refresh_tokens where id in (select id from refresh_tokens where expires_at <= :now or (revoked_at is not null and revoked_at < :revokedBefore) order by id limit :limit for update skip locked)", nativeQuery = true)
    int deleteExpiredOrOldRevoked(@Param("now") LocalDateTime now, @Param("revokedBefore") LocalDateTime revokedBefore, @Param("limit") int limit);
}
