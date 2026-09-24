package com.group01.game.infrastructure.persistence.repository;

import com.group01.game.infrastructure.persistence.entity.GameMatchJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GameMatchJpaRepository extends JpaRepository<GameMatchJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gameMatch from GameMatchJpaEntity gameMatch where gameMatch.id = :id")
    Optional<GameMatchJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
