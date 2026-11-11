package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

public interface AccountActionTokenJpaRepository extends JpaRepository<AccountActionTokenJpaEntity, UUID> {
    Optional<AccountActionTokenJpaEntity> findByTokenHash(String tokenHash);
    Optional<AccountActionTokenJpaEntity> findByTokenHashAndPurpose(String tokenHash, ActionTokenPurpose purpose);
    @Modifying
    @Query("update AccountActionTokenJpaEntity token set token.usedAt = :consumedAt where token.tokenHash = :tokenHash and token.purpose = com.ieltspath.user.domain.vo.ActionTokenPurpose.PASSWORD_RESET and token.usedAt is null and token.expiresAt > :consumedAt")
    int consumePasswordResetToken(@Param("tokenHash") String tokenHash, @Param("consumedAt") java.time.LocalDateTime consumedAt);
    @Modifying
    @Query("update AccountActionTokenJpaEntity token set token.usedAt = :consumedAt where token.tokenHash = :tokenHash and token.purpose = :purpose and token.usedAt is null and token.expiresAt > :consumedAt")
    int consumeToken(@Param("tokenHash") String tokenHash, @Param("purpose") ActionTokenPurpose purpose,
                     @Param("consumedAt") LocalDateTime consumedAt);

    @Modifying
    @Query("update AccountActionTokenJpaEntity token set token.usedAt = :invalidatedAt " +
            "where token.user.id = :userId and token.purpose = :purpose and token.usedAt is null")
    int invalidateUnusedForUser(@Param("userId") UUID userId, @Param("purpose") ActionTokenPurpose purpose,
                                @Param("invalidatedAt") LocalDateTime invalidatedAt);
    @Query("select max(token.createdAt) from AccountActionTokenJpaEntity token " +
            "where token.user.id = :userId and token.purpose = :purpose")
    Optional<LocalDateTime> findLatestCreatedAt(@Param("userId") UUID userId,
                                                 @Param("purpose") ActionTokenPurpose purpose);
    List<AccountActionTokenJpaEntity> findByUser_Id(UUID userId);
    void deleteByUser_Id(UUID userId);
}

