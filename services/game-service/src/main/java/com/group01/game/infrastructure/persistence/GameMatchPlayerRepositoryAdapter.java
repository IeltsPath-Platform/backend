package com.group01.game.infrastructure.persistence;

import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository @Transactional
public class GameMatchPlayerRepositoryAdapter implements GameMatchPlayerRepository {
    private final GameMatchPlayerJpaRepository repository;
    public GameMatchPlayerRepositoryAdapter(GameMatchPlayerJpaRepository repository) { this.repository=repository; }
    @Override public GameMatchPlayer save(GameMatchPlayer p) {
        GameMatchPlayerJpaEntity e=repository.findById(p.id()).orElseGet(()->new GameMatchPlayerJpaEntity(p.id(),p.matchId(),p.roomMemberId(),p.userId(),p.joinedAt()));
        e.updateProgress(p.score(),p.status()==GameMatchPlayer.Status.FINISHED,p.finishedAt()==null?p.joinedAt():p.finishedAt());
        if(p.rank()!=null)e.setRank(p.rank()); repository.save(e); return p;
    }
    @Override @Transactional(readOnly=true) public List<GameMatchPlayer> findByMatchId(UUID id) { return repository.findAllByMatchIdOrderByJoinedAt(id).stream().map(this::toDomain).toList(); }
    @Override @Transactional(readOnly=true) public Optional<GameMatchPlayer> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override @Transactional(readOnly=true) public Optional<GameMatchPlayer> findByMatchIdAndUserId(UUID matchId,UUID userId) { return repository.findByMatchIdAndUserId(matchId,userId).map(this::toDomain); }
    private GameMatchPlayer toDomain(GameMatchPlayerJpaEntity e) { return new GameMatchPlayer(e.getId(),e.getMatchId(),e.getRoomMemberId(),e.getUserId(),e.getScore(),e.getRank(),GameMatchPlayer.Status.valueOf(e.getStatus()),e.getJoinedAt(),e.getFinishedAt()); }
}
