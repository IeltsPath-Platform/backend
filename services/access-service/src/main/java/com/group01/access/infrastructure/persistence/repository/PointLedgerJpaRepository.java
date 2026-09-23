package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.PointLedgerEntryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointLedgerJpaRepository extends JpaRepository<PointLedgerEntryJpaEntity, UUID> {

    Optional<PointLedgerEntryJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<PointLedgerEntryJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
