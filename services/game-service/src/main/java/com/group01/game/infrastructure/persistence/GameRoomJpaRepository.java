package com.group01.game.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GameRoomJpaRepository extends JpaRepository<GameRoomJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from GameRoomJpaEntity room where room.id = :id")
    Optional<GameRoomJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<GameRoomJpaEntity> findByRoomCode(String roomCode);
}
