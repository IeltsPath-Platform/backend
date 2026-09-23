package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.PointWalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PointWalletJpaRepository extends JpaRepository<PointWalletJpaEntity, UUID> {
}
