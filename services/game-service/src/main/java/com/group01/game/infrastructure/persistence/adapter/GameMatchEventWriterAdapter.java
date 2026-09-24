package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.application.port.GameMatchEventWriter;
import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.infrastructure.persistence.entity.GameEventJpaEntity;
import com.group01.game.infrastructure.persistence.repository.GameEventJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
public class GameMatchEventWriterAdapter implements GameMatchEventWriter {
    private final GameEventJpaRepository eventJpaRepository;

    public GameMatchEventWriterAdapter(GameEventJpaRepository eventJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    @Transactional
    public void recordAnswer(UUID matchId, UUID matchPlayerId, GameAnswer answer, GameSession session, Instant occurredAt) {
        long sequence = eventJpaRepository.findMaxSequence(matchId) + 1;
        eventJpaRepository.save(new GameEventJpaEntity(UUID.randomUUID(), matchId, matchPlayerId, sequence,
                "ANSWER_SUBMITTED", Map.of("sessionId", session.id().toString(), "itemSequence", answer.itemSequence(),
                "isCorrect", answer.correct(), "score", session.score()), occurredAt));
    }
}
