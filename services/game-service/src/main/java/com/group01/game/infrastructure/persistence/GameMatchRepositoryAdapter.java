package com.group01.game.infrastructure.persistence;

import com.group01.game.domain.aggregate.GameMatch;
import com.group01.game.domain.repository.GameMatchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository @Transactional
public class GameMatchRepositoryAdapter implements GameMatchRepository {
    private final GameMatchJpaRepository repository;
    public GameMatchRepositoryAdapter(GameMatchJpaRepository repository) { this.repository=repository; }
    @Override public GameMatch save(GameMatch match) {
        GameMatchJpaEntity entity=repository.findById(match.id()).orElseGet(()->new GameMatchJpaEntity(
                match.id(),match.roomId(),match.gameType(),match.learningDomain(),match.configSnapshot(),match.contentSnapshot(),match.createdAt()));
        if(match.status()==GameMatch.Status.COMPLETED && !"COMPLETED".equals(entity.getStatus())) entity.complete(match.endedAt());
        repository.save(entity); return match;
    }
    @Override public Optional<GameMatch> findForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(e->GameMatch.reconstitute(e.getId(),e.getRoomId(),e.getGameType(),e.getLearningDomain(),e.getConfigSnapshot(),e.getContentSnapshot(),GameMatch.Status.valueOf(e.getStatus()),e.getStartedAt(),e.getEndedAt(),e.getCreatedAt()));
    }
    @Override public Optional<GameMatch> findById(UUID id) {
        return repository.findById(id).map(e->GameMatch.reconstitute(e.getId(),e.getRoomId(),e.getGameType(),e.getLearningDomain(),e.getConfigSnapshot(),e.getContentSnapshot(),GameMatch.Status.valueOf(e.getStatus()),e.getStartedAt(),e.getEndedAt(),e.getCreatedAt()));
    }
}
