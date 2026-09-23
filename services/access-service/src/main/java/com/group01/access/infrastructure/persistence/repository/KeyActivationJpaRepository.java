package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.KeyActivationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyActivationJpaRepository extends JpaRepository<KeyActivationJpaEntity, UUID> {

    Optional<KeyActivationJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<KeyActivationJpaEntity> findByKeyId(UUID keyId);

    List<KeyActivationJpaEntity> findByUserIdOrderByActivatedAtDesc(UUID userId);
}
