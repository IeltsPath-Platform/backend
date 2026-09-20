package com.group01.user.infrastructure.persistence.repository;

import com.group01.user.domain.vo.ActionTokenPurpose;
import com.group01.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenJpaRepository extends JpaRepository<AccountActionTokenJpaEntity, UUID> {
    Optional<AccountActionTokenJpaEntity> findByTokenHash(String tokenHash);
    Optional<AccountActionTokenJpaEntity> findByTokenHashAndPurpose(String tokenHash, ActionTokenPurpose purpose);
    List<AccountActionTokenJpaEntity> findByUser_Id(UUID userId);
    void deleteByUser_Id(UUID userId);
}

