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

public interface AccountActionTokenJpaRepository extends JpaRepository<AccountActionTokenJpaEntity, UUID> {
    Optional<AccountActionTokenJpaEntity> findByTokenHash(String tokenHash);
    Optional<AccountActionTokenJpaEntity> findByTokenHashAndPurpose(String tokenHash, ActionTokenPurpose purpose);
    @Modifying
    @Query("update AccountActionTokenJpaEntity token set token.usedAt = :consumedAt where token.tokenHash = :tokenHash and token.purpose = com.ieltspath.user.domain.vo.ActionTokenPurpose.PASSWORD_RESET and token.usedAt is null and token.expiresAt > :consumedAt")
    int consumePasswordResetToken(@Param("tokenHash") String tokenHash, @Param("consumedAt") java.time.LocalDateTime consumedAt);
    List<AccountActionTokenJpaEntity> findByUser_Id(UUID userId);
    void deleteByUser_Id(UUID userId);
}

