package com.group01.game.infrastructure.persistence.mapper;

import com.group01.game.domain.aggregate.GameMatch;
import com.group01.game.infrastructure.persistence.entity.GameMatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GameMatchPersistenceMapper {
    public GameMatch toDomain(GameMatchJpaEntity entity) {
        return GameMatch.reconstitute(entity.getId(), entity.getRoomId(), entity.getGameType(),
                entity.getLearningDomain(), entity.getConfigSnapshot(), entity.getContentSnapshot(),
                GameMatch.Status.valueOf(entity.getStatus()), entity.getStartedAt(), entity.getEndedAt(),
                entity.getCreatedAt());
    }

    public GameMatchJpaEntity toNewEntity(GameMatch match) {
        return new GameMatchJpaEntity(match.id(), match.roomId(), match.gameType(), match.learningDomain(),
                match.configSnapshot(), match.contentSnapshot(), match.createdAt());
    }
}
