package com.ieltspath.access.infrastructure.persistence.repository;

import com.ieltspath.access.domain.vo.KeyStatus;
import com.ieltspath.access.infrastructure.persistence.entity.ActivationKeyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationKeyJpaRepository extends JpaRepository<ActivationKeyJpaEntity, UUID> {

    /**
     * Locks the row until the surrounding transaction ends: two learners redeeming the same code, or a revoke at the
     * same moment, take turns instead of both seeing the key as still active.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivationKeyJpaEntity> findByCodeHash(String codeHash);

    List<ActivationKeyJpaEntity> findByStatus(KeyStatus status, Pageable pageable);

    List<ActivationKeyJpaEntity> findByProductId(UUID productId, Pageable pageable);
}
