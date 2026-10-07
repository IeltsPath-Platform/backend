package com.ieltspath.game.infrastructure.persistence.repository;

import com.ieltspath.game.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}
