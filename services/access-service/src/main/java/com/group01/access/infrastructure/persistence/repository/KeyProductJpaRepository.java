package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.KeyProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyProductJpaRepository extends JpaRepository<KeyProductJpaEntity, UUID> {

    Optional<KeyProductJpaEntity> findByCode(String code);
}
