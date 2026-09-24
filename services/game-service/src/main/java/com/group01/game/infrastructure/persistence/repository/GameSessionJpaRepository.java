package com.group01.game.infrastructure.persistence.repository;

import com.group01.game.infrastructure.persistence.entity.GameSessionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionJpaRepository extends JpaRepository<GameSessionJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameSessionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<GameSessionJpaEntity> findAllByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Optional<GameSessionJpaEntity> findByMatchPlayerId(UUID matchPlayerId);

}
