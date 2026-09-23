package com.group01.game.infrastructure.persistence;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.aggregate.GameSessionStatus;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class GameSessionRepositoryAdapter implements GameSessionRepository {
    private final GameSessionJpaRepository sessionRepository;
    private final GameAnswerJpaRepository answerRepository;

    public GameSessionRepositoryAdapter(GameSessionJpaRepository sessionRepository,
                                        GameAnswerJpaRepository answerRepository) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
    }

    @Override
    public GameSession save(GameSession session) {
        GameSessionJpaEntity entity = sessionRepository.findById(session.id()).orElseGet(() ->
                new GameSessionJpaEntity(session.id(), session.userId(), session.matchPlayerId(), session.gameType(), session.learningDomain(),
                        session.mode(), session.topicId(), session.sourceSnapshot(), session.startedAt()));
        entity.setProgress(session.score(), session.status(), session.endedAt());
        return toDomain(sessionRepository.save(entity), session.answeredCount());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findOwned(UUID id, UUID userId) {
        return sessionRepository.findByIdAndUserId(id, userId).map(entity ->
                toDomain(entity, Math.toIntExact(answerRepository.countBySessionId(entity.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findByMatchPlayerId(UUID matchPlayerId) {
        return sessionRepository.findByMatchPlayerId(matchPlayerId).map(entity ->
                toDomain(entity, Math.toIntExact(answerRepository.countBySessionId(entity.getId()))));
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
                .map(entity -> toDomain(entity, answeredBySession.getOrDefault(entity.getId(), 0))).toList(),
                rows.getTotalElements(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameAnswer> findAnswer(UUID sessionId, int itemSequence) {
        return answerRepository.findBySessionIdAndItemSequence(sessionId, itemSequence).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameAnswer> findAnswers(UUID sessionId) {
        return answerRepository.findAllBySessionIdOrderByItemSequence(sessionId).stream().map(this::toDomain).toList();
    }

    @Override
    public GameAnswer saveAnswer(GameAnswer answer) {
        return toDomain(answerRepository.save(new GameAnswerJpaEntity(answer.id(), answer.sessionId(),
                answer.itemSequence(), answer.vocabularySenseId(), answer.questionVersionId(), answer.itemSnapshot(),
                answer.responsePayload(), answer.correct(), answer.durationMilliseconds())));
    }

    private GameSession toDomain(GameSessionJpaEntity entity, int answeredCount) {
        return GameSession.reconstitute(entity.getId(), entity.getUserId(), entity.getTopicId(),
                entity.getMatchPlayerId(), entity.getGameType(), entity.getLearningDomain(), entity.getMode(),
                entity.getStartedAt(), entity.getEndedAt(), entity.getScore() == null ? 0 : entity.getScore(),
                answeredCount, entity.getSourceSnapshot(), entity.getVerificationStatus(), entity.getStatus());
    }

    private GameAnswer toDomain(GameAnswerJpaEntity entity) {
        return new GameAnswer(entity.getId(), entity.getSessionId(), entity.getItemSequence(),
                entity.getVocabularySenseId(), entity.getQuestionVersionId(), entity.getItemSnapshot(),
                entity.getResponsePayload(), entity.isCorrect(), entity.getDurationMilliseconds());
    }
}
