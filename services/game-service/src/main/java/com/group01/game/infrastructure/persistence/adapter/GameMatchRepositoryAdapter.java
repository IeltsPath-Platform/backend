package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.domain.aggregate.GameMatch;
import com.group01.game.domain.repository.GameMatchRepository;
import com.group01.game.infrastructure.persistence.entity.GameMatchJpaEntity;
import com.group01.game.infrastructure.persistence.mapper.GameMatchPersistenceMapper;
import com.group01.game.infrastructure.persistence.repository.GameMatchJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class GameMatchRepositoryAdapter implements GameMatchRepository {
    private final GameMatchJpaRepository repository;
    private final GameMatchPersistenceMapper mapper;

    public GameMatchRepositoryAdapter(GameMatchJpaRepository repository, GameMatchPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public GameMatch save(GameMatch match) {
        GameMatchJpaEntity entity = repository.findById(match.id()).orElseGet(() -> mapper.toNewEntity(match));
        if (match.status() == GameMatch.Status.COMPLETED && !"COMPLETED".equals(entity.getStatus()))
            entity.complete(match.endedAt());
        repository.save(entity);
        return match;
    }

    @Override
    public Optional<GameMatch> findForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<GameMatch> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
