package com.group01.user.infrastructure.persistence.repository;

import com.group01.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenJpaEntity token
               set token.revokedAt = :consumedAt
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
               and token.expiresAt > :consumedAt
            """)
    int revokeActiveToken(
            @Param("tokenHash") String tokenHash,
            @Param("consumedAt") LocalDateTime consumedAt
    );
}
