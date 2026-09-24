package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.repository.GameSessionRepository;
import com.group01.game.infrastructure.persistence.entity.GameAnswerJpaEntity;
import com.group01.game.infrastructure.persistence.entity.GameSessionJpaEntity;
import com.group01.game.infrastructure.persistence.mapper.GameSessionPersistenceMapper;
import com.group01.game.infrastructure.persistence.repository.GameAnswerJpaRepository;
import com.group01.game.infrastructure.persistence.repository.GameSessionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
@Transactional
public class GameSessionRepositoryAdapter implements GameSessionRepository {
    private final GameSessionJpaRepository sessionRepository;
    private final GameAnswerJpaRepository answerRepository;
    private final GameSessionPersistenceMapper mapper;

    public GameSessionRepositoryAdapter(GameSessionJpaRepository sessionRepository,
                                        GameAnswerJpaRepository answerRepository,
                                        GameSessionPersistenceMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.mapper = mapper;
    }

    @Override
    public GameSession save(GameSession session) {
        GameSessionJpaEntity entity = sessionRepository.findById(session.id()).orElseGet(() -> mapper.toNewEntity(session));
        entity.setProgress(session.score(), session.status(), session.endedAt());
        return mapper.toDomain(sessionRepository.save(entity), session.answeredCount());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findOwned(UUID id, UUID userId) {
        return sessionRepository.findByIdAndUserId(id, userId).map(entity ->
                mapper.toDomain(entity, Math.toIntExact(answerRepository.countBySessionId(entity.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findByMatchPlayerId(UUID matchPlayerId) {
        return sessionRepository.findByMatchPlayerId(matchPlayerId).map(entity ->
                mapper.toDomain(entity, Math.toIntExact(answerRepository.countBySessionId(entity.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public SessionPage findHistory(UUID userId, int page, int size) {
        var rows = sessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size));
        List<UUID> ids = rows.getContent().stream().map(GameSessionJpaEntity::getId).toList();
        Map<UUID, Integer> answeredBySession = new HashMap<>();
        if (!ids.isEmpty()) {
            for (GameAnswerJpaEntity answer : answerRepository.findAllBySessionIdInOrderBySessionIdAscItemSequenceAsc(ids)) {
                answeredBySession.merge(answer.getSessionId(), 1, Integer::sum);
            }
        }
        return new SessionPage(rows.getContent().stream()
                .map(entity -> mapper.toDomain(entity, answeredBySession.getOrDefault(entity.getId(), 0))).toList(),
                rows.getTotalElements(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameAnswer> findAnswer(UUID sessionId, int itemSequence) {
        return answerRepository.findBySessionIdAndItemSequence(sessionId, itemSequence).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameAnswer> findAnswers(UUID sessionId) {
        return answerRepository.findAllBySessionIdOrderByItemSequence(sessionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public GameAnswer saveAnswer(GameAnswer answer) {
        return mapper.toDomain(answerRepository.save(mapper.toNewEntity(answer)));
    }
}
