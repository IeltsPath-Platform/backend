package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.domain.vo.KeyStatus;
import com.group01.access.infrastructure.persistence.entity.ActivationKeyJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationKeyJpaRepository extends JpaRepository<ActivationKeyJpaEntity, UUID> {

    Optional<ActivationKeyJpaEntity> findByCodeHash(String codeHash);

    List<ActivationKeyJpaEntity> findByStatus(KeyStatus status, Pageable pageable);

    List<ActivationKeyJpaEntity> findByProductId(UUID productId, Pageable pageable);
}
