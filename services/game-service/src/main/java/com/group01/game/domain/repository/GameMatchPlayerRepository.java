package com.group01.game.domain.repository;
import com.group01.game.domain.aggregate.GameMatchPlayer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface GameMatchPlayerRepository {
    GameMatchPlayer save(GameMatchPlayer player);

    List<GameMatchPlayer> saveAll(List<GameMatchPlayer> players);
    List<GameMatchPlayer> findByMatchId(UUID matchId);
    Optional<GameMatchPlayer> findById(UUID id);
    Optional<GameMatchPlayer> findByMatchIdAndUserId(UUID matchId, UUID userId);
}
