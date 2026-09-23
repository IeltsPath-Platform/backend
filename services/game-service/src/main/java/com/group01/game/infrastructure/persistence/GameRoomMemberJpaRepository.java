package com.group01.game.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRoomMemberJpaRepository extends JpaRepository<GameRoomMemberJpaEntity, UUID> {
    Optional<GameRoomMemberJpaEntity> findByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByRoomIdAndStatusIn(UUID roomId, Collection<String> statuses);
    List<GameRoomMemberJpaEntity> findAllByRoomIdOrderByJoinedAt(UUID roomId);
    boolean existsByRoomIdAndUserIdAndStatusIn(UUID roomId, UUID userId, Collection<String> statuses);
}
