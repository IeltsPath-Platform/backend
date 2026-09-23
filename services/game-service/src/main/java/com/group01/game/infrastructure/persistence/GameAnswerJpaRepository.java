package com.group01.game.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameAnswerJpaRepository extends JpaRepository<GameAnswerJpaEntity, UUID> {
    boolean existsBySessionIdAndItemSequence(UUID sessionId, int itemSequence);
    Optional<GameAnswerJpaEntity> findBySessionIdAndItemSequence(UUID sessionId, int itemSequence);
    List<GameAnswerJpaEntity> findAllBySessionIdOrderByItemSequence(UUID sessionId);
    List<GameAnswerJpaEntity> findAllBySessionIdInOrderBySessionIdAscItemSequenceAsc(List<UUID> sessionIds);
    long countBySessionId(UUID sessionId);
}
