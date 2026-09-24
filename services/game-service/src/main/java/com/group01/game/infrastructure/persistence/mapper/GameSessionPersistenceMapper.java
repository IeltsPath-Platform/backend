package com.group01.game.infrastructure.persistence.mapper;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.infrastructure.persistence.entity.GameAnswerJpaEntity;
import com.group01.game.infrastructure.persistence.entity.GameSessionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GameSessionPersistenceMapper {
    public GameSession toDomain(GameSessionJpaEntity entity, int answeredCount) {
        return GameSession.reconstitute(entity.getId(), entity.getUserId(), entity.getTopicId(),
                entity.getMatchPlayerId(), entity.getGameType(), entity.getLearningDomain(), entity.getMode(),
                entity.getStartedAt(), entity.getEndedAt(), entity.getScore() == null ? 0 : entity.getScore(),
                answeredCount, entity.getSourceSnapshot(), entity.getVerificationStatus(), entity.getStatus());
    }

    public GameAnswer toDomain(GameAnswerJpaEntity entity) {
        return new GameAnswer(entity.getId(), entity.getSessionId(), entity.getItemSequence(),
                entity.getVocabularySenseId(), entity.getQuestionVersionId(), entity.getItemSnapshot(),
                entity.getResponsePayload(), entity.isCorrect(), entity.getDurationMilliseconds());
    }

    public GameSessionJpaEntity toNewEntity(GameSession session) {
        return new GameSessionJpaEntity(session.id(), session.userId(), session.matchPlayerId(), session.gameType(),
                session.learningDomain(), session.mode(), session.topicId(), session.sourceSnapshot(), session.startedAt());
    }

    public GameAnswerJpaEntity toNewEntity(GameAnswer answer) {
        return new GameAnswerJpaEntity(answer.id(), answer.sessionId(), answer.itemSequence(),
                answer.vocabularySenseId(), answer.questionVersionId(), answer.itemSnapshot(),
                answer.responsePayload(), answer.correct(), answer.durationMilliseconds());
    }
}
