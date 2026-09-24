package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import com.group01.game.infrastructure.persistence.entity.GameMatchPlayerJpaEntity;
import com.group01.game.infrastructure.persistence.mapper.GameMatchPlayerPersistenceMapper;
import com.group01.game.infrastructure.persistence.repository.GameMatchPlayerJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class GameMatchPlayerRepositoryAdapter implements GameMatchPlayerRepository {
    private final GameMatchPlayerJpaRepository repository;
    private final GameMatchPlayerPersistenceMapper mapper;

    public GameMatchPlayerRepositoryAdapter(GameMatchPlayerJpaRepository repository, GameMatchPlayerPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public GameMatchPlayer save(GameMatchPlayer p) {
        GameMatchPlayerJpaEntity e = repository.findById(p.id()).orElseGet(() -> mapper.toNewEntity(p));
        e.updateProgress(p.score(), p.status() == GameMatchPlayer.Status.FINISHED, p.finishedAt() == null ? p.joinedAt() : p.finishedAt());
        if (p.rank() != null) e.setRank(p.rank());
        repository.save(e);
        return p;
    }

    @Override
    public List<GameMatchPlayer> saveAll(List<GameMatchPlayer> players) {
        if (players.isEmpty()) return List.of();
        var entitiesById = repository.findAllById(players.stream().map(GameMatchPlayer::id).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(GameMatchPlayerJpaEntity::getId, entity -> entity));
        for (GameMatchPlayer player : players) {
            GameMatchPlayerJpaEntity entity = entitiesById.get(player.id());
            if (entity == null) throw new IllegalStateException("Game match player not found: " + player.id());
            entity.setRank(player.rank());
        }
        repository.saveAll(entitiesById.values());
        return players;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameMatchPlayer> findByMatchId(UUID id) {
        return repository.findAllByMatchIdOrderByJoinedAt(id).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameMatchPlayer> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameMatchPlayer> findByMatchIdAndUserId(UUID matchId, UUID userId) {
        return repository.findByMatchIdAndUserId(matchId, userId).map(mapper::toDomain);
    }
}
